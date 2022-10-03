package info.blockchain.wallet.payload

import com.blockchain.AppVersion
import com.blockchain.api.ApiException
import com.blockchain.api.services.NonCustodialBitcoinService
import com.blockchain.logging.RemoteLogger
import com.blockchain.serialization.JsonSerializableAccount
import info.blockchain.balance.Money
import info.blockchain.wallet.Device
import info.blockchain.wallet.api.WalletApi
import info.blockchain.wallet.exceptions.AccountLockedException
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.EncryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import info.blockchain.wallet.exceptions.NoSuchAddressException
import info.blockchain.wallet.exceptions.ServerConnectionException
import info.blockchain.wallet.exceptions.UnsupportedVersionException
import info.blockchain.wallet.keys.MasterKey
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.keys.SigningKeyImpl
import info.blockchain.wallet.multiaddress.MultiAddressFactory
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.pairing.Pairing
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.Derivation
import info.blockchain.wallet.payload.data.ImportedAddress
import info.blockchain.wallet.payload.data.Wallet
import info.blockchain.wallet.payload.data.WalletBase
import info.blockchain.wallet.payload.data.WalletBody
import info.blockchain.wallet.payload.data.WalletBody.Companion.recoverFromMnemonic
import info.blockchain.wallet.payload.data.WalletWrapper
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.data.activeXpubs
import info.blockchain.wallet.payload.data.nonArchivedImportedAddressStrings
import info.blockchain.wallet.payload.model.Balance
import info.blockchain.wallet.payload.model.toBalanceMap
import info.blockchain.wallet.util.DoubleEncryptionFactory
import info.blockchain.wallet.util.Tools
import java.io.IOException
import java.math.BigInteger
import java.security.NoSuchAlgorithmException
import org.apache.commons.codec.DecoderException
import org.bitcoinj.crypto.MnemonicException.MnemonicChecksumException
import org.bitcoinj.crypto.MnemonicException.MnemonicLengthException
import org.bitcoinj.crypto.MnemonicException.MnemonicWordException
import org.slf4j.LoggerFactory
import org.spongycastle.crypto.InvalidCipherTextException
import org.spongycastle.util.encoders.Hex

class PayloadManager(
    private val walletApi: WalletApi,
    private val bitcoinApi: NonCustodialBitcoinService,
    private val multiAddressFactory: MultiAddressFactory,
    private val balanceManagerBtc: BalanceManagerBtc,
    private val balanceManagerBch: BalanceManagerBch,
    private val device: Device,
    private val remoteLogger: RemoteLogger,
    private val appVersion: AppVersion,
) {
    private lateinit var walletBase: WalletBase
    private lateinit var password: String

    val payload: Wallet
        get() = walletBase.wallet

    private fun updatePayload(walletBase: WalletBase) {
        this.walletBase = walletBase
    }

    val payloadChecksum: String?
        get() = walletBase.payloadChecksum

    val tempPassword: String
        get() = password

    private fun setTempPassword(password: String) {
        this.password = password
    }

    /**
     * Currently checked only for notifications, as we need a way to know if paylaod is in place when
     * a new token from firebase is sent.
     *
     */
    fun initialised(): Boolean =
        this::walletBase.isInitialized && walletBase.isInitialised()

    fun create(
        defaultAccountName: String,
        email: String,
        password: String,
        recaptchaToken: String?
    ): Wallet {
        this.password = password
        walletBase = WalletBase(
            Wallet(
                defaultAccountName
            )
        )
        saveNewWallet(walletBase, email, recaptchaToken)
        return payload
    }

    private fun saveNewWallet(walletBase: WalletBase, email: String, recaptchaToken: String?) {
        validateSave(walletBase)
        // Encrypt and wrap payload
        val (newPayloadChecksum, payloadWrapper) = walletBase.encryptAndWrapPayload(password)
        // Save to server
        val call = walletApi.insertWallet(
            payload.guid,
            payload.sharedKey,
            null,
            payloadWrapper.toJson(),
            newPayloadChecksum,
            email,
            device.osType,
            recaptchaToken
        )
        val exe = call.execute()
        if (exe.isSuccessful) {
            // set new checksum
            updatePayload(this.walletBase.withUpdatedChecksum(newPayloadChecksum))
        } else {
            log.error("", exe.code().toString() + " - " + exe.errorBody()!!.string())
            throw ServerConnectionException(exe.code().toString() + " - " + exe.errorBody()!!.string())
        }
    }

    /**
     * Creates a new Blockchain wallet based on provided mnemonic and saves it to the server.
     *
     * @param mnemonic 12 word recovery phrase - space separated
     * @param email    Used to send GUID link to user
     */
    fun recoverFromMnemonic(
        mnemonic: String,
        defaultAccountName: String,
        email: String,
        password: String
    ): Wallet {
        this.password = password
        val walletBody = recoverFromMnemonic(
            mnemonic,
            defaultAccountName,
            bitcoinApi
        )
        val wallet = Wallet(walletBody)
        walletBase = WalletBase(wallet)
        saveNewWallet(walletBase, email, null)
        return payload
    }

    /**
     * Initializes a wallet from provided credentials.
     * Calls balance api to show wallet balances on wallet load.
     *
     * @throws InvalidCredentialsException GUID might be incorrect
     * @throws AccountLockedException      Account has been locked, contact support
     * @throws ServerConnectionException   Unknown server error
     * @throws DecryptionException         Password not able to decrypt payload
     * @throws InvalidCipherTextException  Decryption issue
     * @throws UnsupportedVersionException Payload version newer than current supported
     * @throws MnemonicLengthException     Initializing HD issue
     * @throws MnemonicWordException       Initializing HD issue
     * @throws MnemonicChecksumException   Initializing HD issue
     * @throws DecoderException            Decryption issue
     */
    fun initializeAndDecrypt(
        sharedKey: String,
        guid: String,
        password: String
    ) {
        this.password = password
        val call = walletApi.fetchWalletData(guid, sharedKey)
        val exe = call.execute()
        walletBase = if (exe.isSuccessful) {
            WalletBase.fromJson(exe.body()!!.string()).withDecryptedPayload(this.password)
        } else {
            log.warn("Fetching wallet data failed with provided credentials")
            val errorMessage = exe.errorBody()!!.string()
            log.warn("", errorMessage)
            if (errorMessage.contains("Unknown Wallet Identifier")) {
                throw InvalidCredentialsException()
            } else if (errorMessage.contains("locked")) {
                throw AccountLockedException(errorMessage)
            } else {
                throw ServerConnectionException(errorMessage)
            }
        }
    }

    fun initializeAndDecryptFromQR(
        qrData: String
    ) {
        val qrComponents = Pairing.qRComponentsFromRawString(qrData)
        val call = walletApi.fetchPairingEncryptionPasswordCall(qrComponents.first)
        val exe = call.execute()
        if (exe.isSuccessful) {
            val encryptionPassword = exe.body()!!.string()
            val encryptionPairingCode = qrComponents.second as String
            val guid = qrComponents.first
            val sharedKeyAndPassword = Pairing.sharedKeyAndPassword(encryptionPairingCode, encryptionPassword)
            val sharedKey = sharedKeyAndPassword[0]
            val hexEncodedPassword = sharedKeyAndPassword[1]
            val password = String(Hex.decode(hexEncodedPassword), Charsets.UTF_8)
            initializeAndDecrypt(
                sharedKey,
                guid,
                password
            )
        } else {
            log.error("", exe.code().toString() + " - " + exe.errorBody()!!.string())
            throw ServerConnectionException(exe.code().toString() + " - " + exe.errorBody()!!.string())
        }
    }

    val isWalletBackedUp: Boolean
        get() =
            payload.walletBodies.takeIf {
                it?.isNotEmpty() == true
            }?.let {
                it[0].mnemonicVerified
            } ?: false

    /**
     * Upgrades a V2 wallet to a V3 HD wallet and saves it to the server
     * NB! When called from Android - First apply PRNGFixes
     */
    fun upgradeV2PayloadToV3(
        secondPassword: String?,
        defaultAccountName: String
    ) {
        val currentWallet = payload
        try {
            val upgradedWallet = currentWallet.upgradeV2PayloadToV3(secondPassword, defaultAccountName)
            val success: Boolean = saveAndSync(walletBase.withWalletBody(upgradedWallet), password)
            if (!success) {
                throw Exception("Save failed")
            }
        } catch (t: Throwable) {
            throw t
        }
    }

    /**
     * Upgrades a V3 wallet to V4 wallet format and saves it to the server
     *
     * @param secondPassword
     * @throws Exception
     */
    fun upgradeV3PayloadToV4(secondPassword: String?) {
        if (payload.isDoubleEncryption) {
            payload.decryptHDWallet(secondPassword)
        }

        val v4WalletBodies = payload.walletBodies?.map { v3walletBody ->
            v3walletBody.upgradeAccountsToV4(
                secondPassword,
                payload.sharedKey,
                payload.options.pbkdf2Iterations!!
            )
        } ?: emptyList()
        val success =
            saveAndSync(
                walletBase.withWalletBody(payload.withUpdatedBodiesAndVersion(v4WalletBodies, 4)), password
            )
        if (!success) {
            throw Exception("Save failed")
        }
    }

    fun updateDerivationsForAccounts(accounts: List<Account>) {
        saveAndSync(
            walletBase.withUpdatedDerivationsForAccounts(accounts), password
        )
    }

    fun updateAccountLabel(account: JsonSerializableAccount, label: String) {
        saveAndSync(
            walletBase.withUpdatedLabel(account, label), password
        )
    }

    fun updateArchivedAccountState(account: JsonSerializableAccount, acrhived: Boolean) {
        saveAndSync(
            walletBase.withUpdatedAccountState(account, acrhived), password
        )
    }

    fun updateMnemonicVerified(verified: Boolean) {
        saveAndSync(
            walletBase.withMnemonicState(verified), password
        )
    }

    fun updatePassword(password: String): Boolean {
        val success: Boolean = saveAndSync(walletBase, password)
        if (success) {
            this.password = password
        }
        return success
    }

    fun updateDefaultIndex(index: Int) {
        saveAndSync(
            walletBase.withWalletBody(payload.updateDefaultIndex(index)),
            password
        )
    }

    fun updateDefaultDerivationTypeForAccounts(accounts: List<Account>) {
        saveAndSync(
            walletBase.withUpdatedDefaultDerivationTypeForAccounts(accounts),
            password
        )
    }

    private fun validateSave(walletBase: WalletBase) {
        if (!walletBase.wallet.isEncryptionConsistent) {
            throw HDWalletException("Save aborted - Payload corrupted. Key encryption not consistent.")
        }
    }

    /**
     * Initializes a wallet from a Payload string from manual pairing. Should decode both V3 and V1 wallets successfully.
     *
     * @param networkParameters The parameters for the network - TestNet or MainNet
     * @param payload           The Payload in String format that you wish to decrypt and initialise
     * @param password          The password for the payload
     * @throws HDWalletException   Thrown for a variety of reasons, wraps actual exception and is fatal
     * @throws DecryptionException Thrown if the password is incorrect
     */
    fun initializeAndDecryptFromPayload(
        payload: String,
        password: String
    ) {
        try {
            walletBase = WalletBase.fromJson(payload).withDecryptedPayload(password)
            setTempPassword(password)
        } catch (decryptionException: DecryptionException) {
            log.warn("", decryptionException)
            remoteLogger.logException(decryptionException, "")
            throw decryptionException
        } catch (e: java.lang.Exception) {
            log.error("", e)
            remoteLogger.logException(e, "")
            throw HDWalletException(e)
        }
    }

    /**
     * Saves wallet to server and forces the upload of the user's addresses to allow notifications
     * to work correctly.
     *
     * @return True if save successful
     */
    fun syncPubKeys(): Boolean {
        return saveAndSync(
            walletBase.withSyncedPubKeys(),
            password
        )
    }

    /**
     * Saves the payload to the API and if the save was successfull
     * it updates the local one with the updated
     * and returns true
     * If save fails, then it returns false.
     *
     * @param walletBase
     * @return
     * @throws HDWalletException
     * @throws NoSuchAlgorithmException
     * @throws EncryptionException
     * @throws IOException
     */
    private fun saveAndSync(newWalletBase: WalletBase, passw: String): Boolean {
        validateSave(newWalletBase)
        // Encrypt and wrap payload
        val payloadVersion = newWalletBase.wallet.wrapperVersion
        val (newPayloadChecksum, payloadWrapper) = newWalletBase.encryptAndWrapPayload(passw)
        val oldPayloadChecksum = walletBase.payloadChecksum

        // Save to server
        val syncAddresses = if (newWalletBase.syncPubkeys) {
            makePubKeySyncList(payload.walletBody!!, payloadVersion)
        } else {
            emptyList()
        }

        val call = walletApi.updateWallet(
            guid = payload.guid,
            sharedKey = payload.sharedKey,
            activeAddressList = syncAddresses,
            encryptedPayload = payloadWrapper.toJson(),
            newChecksum = newPayloadChecksum,
            oldChecksum = oldPayloadChecksum,
            device = device.osType
        )
        val exe = call.execute()
        return if (exe.isSuccessful) {
            // set new checksum and update the local
            val updatedWalletBase = newWalletBase.withUpdatedChecksum(newPayloadChecksum)
            updatePayload(updatedWalletBase)
            true
        } else {
            log.error("Save unsuccessful: " + exe.errorBody()!!.string())
            false
        }
    }

    private fun makePubKeySyncList(
        walletBody: WalletBody,
        payloadVersion: Int
    ): List<String> {

        // This matches what iOS is doing, but it seems to be massive overkill for mobile
        // devices. I'm also filtering out archived accounts here because I don't see the point
        // in sending them.
        val derivationPurpose =
            if (payloadVersion == WalletWrapper.V4) Derivation.SEGWIT_BECH32_PURPOSE else Derivation.LEGACY_PURPOSE

        return walletBody.accounts.filterNot { it.isArchived }.mapNotNull { account ->
            val hdAccount = walletBody.getHDAccountFromAccountBody(account)[0] ?: return@mapNotNull null
            val nextIndex: Int = getNextReceiveAddressIndexBtc(account)
            Tools.getReceiveAddressList(
                hdAccount,
                nextIndex,
                nextIndex + 20,
                derivationPurpose
            )
        }.flatten().plus(
            Tools.filterImportedAddress(
                ImportedAddress.NORMAL_ADDRESS,
                payload.importedAddressList
            )
        )
    }

    // /////////////////////////////////////////////////////////////////////////
    // ACCOUNT AND IMPORTED HDADDRESS CREATION
    // /////////////////////////////////////////////////////////////////////////
    /**
     * Adds a new account to hd wallet and saves to server.
     * Reverts on save failure.
     */
    fun addAccount(
        label: String,
        secondPassword: String?
    ): Account {

        val updatedWallet: Wallet = payload.addAccount(
            label,
            secondPassword
        )
        val success = saveAndSync(
            walletBase.withWalletBody(updatedWallet),
            password
        )
        if (!success) {
            // Revert on save fail
            throw java.lang.Exception("Failed to save added account.")
        }
        updateAllBalances()
        return walletBase.wallet.walletBody!!.lastCreatedAccount()
    }

    /**
     * Inserts a [ImportedAddress] into the user's [Wallet] and then syncs the wallet with
     * the server.
     *
     * @param importedAddress The [ImportedAddress] to be added
     * @throws Exception Possible if saving the Wallet fails
     */
    fun addImportedAddress(importedAddress: ImportedAddress?) {
        val wallet: Wallet = payload.addImportedAddress(importedAddress)
        val updatedWalletBase = walletBase.withWalletBody(wallet)
        if (!saveAndSync(
                updatedWalletBase,
                password
            )
        ) {
            throw java.lang.Exception("Failed to save added Imported Address.")
        }
        updateAllBalances()
    }

    /**
     * Sets private key to existing matching imported address. If no match is found the key will be added
     * to the wallet non the less.
     *
     * @param key            ECKey for existing imported address
     * @param secondPassword Double encryption password if applicable.
     */
    fun setKeyForImportedAddress(
        key: SigningKey,
        secondPassword: String?
    ): ImportedAddress {
        return try {
            val address = payload.updateKeyForImportedAddress(
                key,
                secondPassword
            )
            if (saveAndSync(
                    walletBase.withWalletBody(payload.replaceOrAddImportedAddress(address)),
                    password
                )
            ) {
                address
            } else {
                throw RuntimeException("Failed to add address")
            }
        } catch (e: NoSuchAddressException) { // No match found
            addImportedAddressFromKey(key, secondPassword)
        }
    }

    fun addImportedAddressFromKey(
        key: SigningKey,
        secondPassword: String?
    ): ImportedAddress {
        val address = payload.importedAddressFromKey(
            key,
            secondPassword, device.osType,
            appVersion.appVersion
        )
        val wallet: Wallet = payload.addImportedAddress(address)
        return if (saveAndSync(
                walletBase.withWalletBody(wallet),
                password
            )
        ) {
            address
        } else {
            throw RuntimeException("Failed to import Address")
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // SHORTCUT METHODS
    // /////////////////////////////////////////////////////////////////////////

    // /////////////////////////////////////////////////////////////////////////
    // SHORTCUT METHODS
    // /////////////////////////////////////////////////////////////////////////
    fun validateSecondPassword(secondPassword: String?): Boolean {
        return try {
            payload.validateSecondPassword(secondPassword)
            true
        } catch (e: java.lang.Exception) {
            log.warn("", e)
            e.printStackTrace()
            false
        }
    }

    fun isV3UpgradeRequired(): Boolean {
        return !payload.isUpgradedToV3
    }

    fun isV4UpgradeRequired(): Boolean {
        val payloadVersion = payload.wrapperVersion
        return payloadVersion < WalletWrapper.V4
    }

    fun getAddressSigningKey(
        importedAddress: ImportedAddress,
        secondPassword: String?
    ): SigningKey {
        payload.validateSecondPassword(secondPassword)
        val decryptedPrivateKey = if (secondPassword != null) {
            DoubleEncryptionFactory
                .decrypt(
                    importedAddress.privateKey,
                    payload.sharedKey,
                    secondPassword,
                    payload.options.pbkdf2Iterations!!
                )
        } else importedAddress.privateKey

        return SigningKeyImpl(
            Tools.getECKeyFromKeyAndAddress(
                decryptedPrivateKey!!, importedAddress.address
            )
        )
    }

    private fun accountTotalBalance(
        balanceHashMap: HashMap<String, Balance>,
        legacyXpub: String,
        segwitXpub: String
    ): Balance? {
        var totalBalance: Balance? = null
        if (balanceHashMap.containsKey(legacyXpub)) {
            totalBalance = balanceHashMap[legacyXpub]
        }
        if (balanceHashMap.containsKey(segwitXpub)) {
            if (totalBalance != null) {
                totalBalance.finalBalance = totalBalance.finalBalance.add(balanceHashMap[segwitXpub]!!.finalBalance)
                totalBalance.totalReceived = totalBalance.totalReceived.add(balanceHashMap[segwitXpub]!!.totalReceived)
                totalBalance.txCount = totalBalance.txCount + balanceHashMap[segwitXpub]!!.txCount
            } else {
                totalBalance = balanceHashMap[segwitXpub]
            }
        }
        return totalBalance
    }

    /**
     * Returns a [Map] of [Balance] objects keyed to their respective Bitcoin
     * Cash addresses.
     *
     * @param addresses A List of Bitcoin Cash addresses as Strings
     * @return A [LinkedHashMap] where they key is the address String, and the value is a
     * [Balance] object
     * @throws IOException  Thrown if there are network issues
     * @throws ApiException Thrown if the call isn't successful
     */
    fun getBalanceOfBchAccounts(xpubs: List<XPubs>): Map<String, Balance> {
        val response = balanceManagerBch.getBalanceOfAddresses(xpubs)
            .execute()
        return if (response.isSuccessful) {
            val result = response.body()!!
            result.toBalanceMap()
        } else {
            throw ApiException(response.code().toString() + ": " + response.errorBody()!!.string())
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // MULTIADDRESS
    // /////////////////////////////////////////////////////////////////////////

    // /////////////////////////////////////////////////////////////////////////
    // MULTIADDRESS
    // /////////////////////////////////////////////////////////////////////////
    /**
     * Gets BTC transaction list for all wallet accounts/addresses
     *
     * @param limit  Amount of transactions per page
     * @param offset Page offset
     * @return List of tx summaries for all wallet transactions
     */
    fun getAllTransactions(limit: Int, offset: Int): List<TransactionSummary> {
        val activeXpubs: List<XPubs> = payload.walletBody!!.getActiveXpubs()
        return multiAddressFactory.getAccountTransactions(
            activeXpubs,
            null,
            limit,
            offset,
            0
        )
    }

    fun masterKey(): MasterKey {
        return try {
            if (payload.isDoubleEncryption && payload.walletBody?.getMasterKey() == null) {
                throw HDWalletException("Wallet private key unavailable. First decrypt with second password.")
            }
            payload.walletBody!!.getMasterKey()!!
        } catch (e: HDWalletException) {
            throw HDWalletException("Wallet private key unavailable. First decrypt with second password.")
        }
    }

    /**
     * Gets BTC transaction list for an [Account].
     *
     * @param xpub   The xPub to get transactions from
     * @param limit  Amount of transactions per page
     * @param offset Page offset
     * @return List of BTC tx summaries for specified xpubs transactions
     */
    fun getAccountTransactions(
        xpubs: XPubs,
        limit: Int,
        offset: Int
    ): List<TransactionSummary> {

        return multiAddressFactory.getAccountTransactions(
            listOf(xpubs),
            null,
            limit,
            offset,
            0
        )
    }

    /**
     * Calculates if an address belongs to any xpubs in wallet. Accepts both BTC and BCH addresses.
     * Make sure multi address is up to date before executing this method.
     *
     * @param address Either a BTC or BCH address
     * @return A boolean, true if the address belongs to an xPub
     */
    fun isOwnHDAddress(address: String): Boolean {
        return multiAddressFactory.isOwnHDAddress(address)
    }

    /**
     * Converts any Bitcoin address to a label.
     *
     * @param address Accepts account receive or change chain address, as well as imported address.
     * @return Account or imported address label
     */
    fun getLabelFromAddress(address: String): String {
        var label: String?
        val xpub = multiAddressFactory.getXpubFromAddress(address)
        label = if (xpub != null) {
            payload.walletBody!!.getLabelFromXpub(xpub)
        } else {
            payload.getLabelFromImportedAddress(address)
        }
        if (label == null || label.isEmpty()) {
            label = address
        }
        return label
    }

    /**
     * Returns an xPub from an address if the address belongs to this wallet.
     *
     * @param address The address you want to query
     * @return An xPub as a String
     */
    fun getXpubFromAddress(address: String): String? {
        return multiAddressFactory.getXpubFromAddress(address)
    }

    /**
     * Gets next BTC receive address. Excludes reserved BTC addresses.
     *
     * @param account The account from which to derive an address
     * @return A BTC address
     */
    fun getNextReceiveAddress(account: Account): String? {
        val derivationType: String = derivationTypeFromXPub(account.xpubs.default)
        val nextIndex = getNextReceiveAddressIndexBtc(account)
        return getReceiveAddress(account, nextIndex, derivationType)
    }

    /**
     * Allows you to generate a BTC receive address at an arbitrary number of positions on the chain
     * from the next valid unused address. For example, the passing 5 as the position will generate
     * an address which correlates with the next available address + 5 positions.
     *
     * @param account  The [Account] you wish to generate an address from
     * @param position Represents how many positions on the chain beyond what is already used that
     * you wish to generate
     * @return A Bitcoin address
     */
    fun getReceiveAddressAtPosition(account: Account, position: Int): String? {
        val derivationType: String = derivationTypeFromXPub(account.xpubs.default)
        val nextIndex = getNextReceiveAddressIndexBtc(account)
        return getReceiveAddressAtArbitraryPosition(account, nextIndex + position, derivationType)
    }

    private fun derivationTypeFromXPub(xpub: XPub): String =
        when (xpub.derivation) {
            XPub.Format.LEGACY -> Derivation.LEGACY_TYPE
            XPub.Format.SEGWIT -> Derivation.SEGWIT_BECH32_TYPE
        }

    /**
     * Returns the position on the receive chain of the next available receive address.
     *
     * @param account The [Account] you wish to generate an address from
     * @return The position of the next available receive address
     */
    fun getPositionOfNextReceiveAddress(account: Account): Int =
        getNextReceiveAddressIndexBtc(account)

    /**
     * Allows you to generate a BTC or BCH address from any given point on the receive chain.
     *
     * @param account  The [Account] you wish to generate an address from
     * @param position What position on the chain the address you wish to create is
     * @return A Bitcoin or Bitcoin Cash address
     */
    fun getReceiveAddressAtArbitraryPosition(account: Account, position: Int, derivationType: String): String? {
        return try {
            getReceiveAddress(account, position, derivationType)
        } catch (e: HDWalletException) {
            null
        }
    }

    private fun getNextReceiveAddressIndexBtc(account: Account): Int {
        return multiAddressFactory.getNextReceiveAddressIndex(
            account.xpubs.default.address,
            account.addressLabels
        )
    }

    private fun getNextChangeAddressIndexBtc(account: Account, derivation: String): Int {
        return multiAddressFactory.getNextChangeAddressIndex(account.xpubForDerivation(derivation)!!)
    }

    private fun getReceiveAddress(
        account: Account,
        position: Int,
        derivationType: String
    ): String? {
        val hdAccount = payload.walletBody!!
            .getHDAccountFromAccountBody(account)[if (derivationType === Derivation.LEGACY_TYPE) 0 else 1]
            ?: return null

        return hdAccount.receive
            .getAddressAt(
                position,
                if (derivationType === Derivation.LEGACY_TYPE)
                    Derivation.LEGACY_PURPOSE else
                    Derivation.SEGWIT_BECH32_PURPOSE
            ).formattedAddress
    }

    private fun getChangeAddress(account: Account, position: Int, derivationType: String): String? {
        val hdAccount = payload
            .walletBody?.getHDAccountFromAccountBody(account)
            ?.get(if (derivationType === Derivation.LEGACY_TYPE) 0 else 1) ?: return null
        return hdAccount.change
            .getAddressAt(
                position,
                if (derivationType === Derivation.LEGACY_TYPE) {
                    Derivation.LEGACY_PURPOSE
                } else {
                    Derivation.SEGWIT_BECH32_PURPOSE
                }
            ).formattedAddress
    }

    /**
     * Gets next BTC change address in the chain.
     *
     * @param account The [Account] from which you wish to derive a change address
     * @return A Bitcoin change address
     */
    fun getNextChangeAddress(account: Account): String? {
        val derivationType = derivationTypeFromXPub(account.xpubs.default)
        val nextIndex = getNextChangeAddressIndexBtc(account, derivationType)
        return getChangeAddress(account, nextIndex, derivationType)
    }

    fun incrementNextReceiveAddress(account: Account) {
        multiAddressFactory.incrementNextReceiveAddress(
            account.xpubs.default,
            account.addressLabels
        )
    }

    fun incrementNextChangeAddress(account: Account) {
        multiAddressFactory.incrementNextChangeAddress(account.xpubs.default.address)
    }

    fun getNextReceiveAddressAndReserve(account: Account, reserveLabel: String): String? {
        val derivationType = derivationTypeFromXPub(account.xpubs.default)
        val nextIndex = getNextReceiveAddressIndexBtc(account)
        reserveAddress(account, nextIndex, reserveLabel)
        return getReceiveAddress(account, nextIndex, derivationType)
    }

    fun reserveAddress(account: Account, index: Int, label: String?) {
        val updatedAccount = account.addAddressLabel(index, label!!)
        val success = saveAndSync(
            walletBase.withWalletBody(payload.updateAccount(account, updatedAccount)),
            password
        )
        if (!success) {
            throw ServerConnectionException("Unable to reserve address.")
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // BALANCE BITCOIN
    // /////////////////////////////////////////////////////////////////////////
    fun getAddressBalance(xpubs: XPubs): Money {
        return balanceManagerBtc.getAddressBalance(xpubs)
    }

    /**
     * Balance API - Final balance for all accounts + addresses.
     */
    fun getWalletBalance(): BigInteger =
        balanceManagerBtc.walletBalance

    /**
     * Updates all account and address balances and transaction counts.
     * API call uses the Balance endpoint and is much quicker than multiaddress.
     * This will allow the wallet to display wallet/account totals while transactions are still being fetched.
     * This also stores the amount of transactions per address which we can use to limit the calls to multiaddress
     * when the limit is reached.
     */
    fun updateAllBalances() {
        val xpubs = payload.activeXpubs()
        val allLegacy = payload.nonArchivedImportedAddressStrings()
        balanceManagerBtc.updateAllBalances(xpubs, allLegacy)
    }

    /**
     * Updates address balance as well as wallet balance.
     * This is used to immediately update balances after a successful transaction which speeds
     * up the balance the UI reflects without the need to wait for incoming websocket notification.
     */
    fun subtractAmountFromAddressBalance(address: String, amount: BigInteger) {
        balanceManagerBtc.subtractAmountFromAddressBalance(address, amount)
    }

    fun updateNotesForTxHash(transactionHash: String, notes: String) {
        saveAndSync(
            walletBase.withWalletBody(payload.updateTxNotes(transactionHash, notes)),
            password
        )
    }

    /**
     * Balance API - Final balance imported addresses.
     */
    val importedAddressesBalance: BigInteger
        get() = balanceManagerBtc.importedAddressesBalance
}

private val log = LoggerFactory.getLogger(PayloadManager::class.java)
