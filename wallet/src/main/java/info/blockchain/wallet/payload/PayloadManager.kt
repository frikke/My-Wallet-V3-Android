package info.blockchain.wallet.payload

import com.blockchain.AppVersion
import com.blockchain.api.ApiException
import com.blockchain.api.services.NonCustodialBitcoinService
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.asSingle
import com.blockchain.internalnotifications.NotificationEvent
import com.blockchain.internalnotifications.NotificationTransmitter
import com.blockchain.logging.RemoteLogger
import com.blockchain.serialization.JsonSerializableAccount
import com.blockchain.utils.thenSingle
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
import info.blockchain.wallet.payload.store.PayloadDataStore
import info.blockchain.wallet.payload.store.WalletPayloadCredentials
import info.blockchain.wallet.util.DoubleEncryptionFactory
import info.blockchain.wallet.util.Tools
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
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
import retrofit2.HttpException

class PayloadManager constructor(
    private val walletApi: WalletApi,
    private val payloadDataStore: PayloadDataStore,
    private val bitcoinApi: NonCustodialBitcoinService,
    private val notificationTransmitter: NotificationTransmitter,
    private val multiAddressFactory: MultiAddressFactory,
    private val balanceManagerBtc: BalanceManagerBtc,
    private val balanceManagerBch: BalanceManagerBch,
    private val device: Device,
    private val remoteLogger: RemoteLogger,
    private val appVersion: AppVersion
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
    ): Single<Wallet> {
        this.password = password
        val walletBase = WalletBase(
            Wallet(
                defaultAccountName
            )
        )
        return saveNewWallet(walletBase, email, recaptchaToken).thenSingle {
            Single.just(payload)
        }
    }

    private fun saveNewWallet(walletBase: WalletBase, email: String, recaptchaToken: String?): Completable {
        validateSave(walletBase)
        // Encrypt and wrap payload
        val (newPayloadChecksum, payloadWrapper) = walletBase.encryptAndWrapPayload(password)
        // Save to server
        return walletApi.insertWallet(
            walletBase.wallet.guid,
            walletBase.wallet.sharedKey,
            null,
            payloadWrapper.toJson(),
            newPayloadChecksum,
            email,
            device.osType,
            recaptchaToken
        ).doOnComplete {
            this.walletBase = walletBase.withUpdatedChecksum(newPayloadChecksum)
        }.onErrorResumeNext {
            if (it is HttpException) {
                Completable.error(
                    ServerConnectionException(it.code().toString() + " - " + it.response()?.errorBody()!!.string())
                )
            } else Completable.error(it)
        }.doFinally {
            notificationTransmitter.postEvent(NotificationEvent.PayloadUpdated)
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
    ): Single<Wallet> {
        this.password = password
        val walletBody = recoverFromMnemonic(
            mnemonic,
            defaultAccountName,
            bitcoinApi
        )
        val wallet = Wallet(walletBody)
        walletBase = WalletBase(wallet)
        return saveNewWallet(walletBase, email, null).thenSingle {
            Single.just(
                payload
            )
        }
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
        password: String,
        sessionId: String
    ): Completable {
        this.password = password
        return payloadDataStore.stream(
            request = FreshnessStrategy.Cached(refreshStrategy = RefreshStrategy.RefreshIfStale).withKey(
                WalletPayloadCredentials(
                    guid,
                    sharedKey,
                    sessionId
                )
            )
        ).asSingle().doOnSuccess {
            walletBase = WalletBase(it).withDecryptedPayload(this.password)
        }.ignoreElement().onErrorResumeNext {
            if (it is HttpException) {
                val message = it.response()?.errorBody()?.string() ?: ""
                return@onErrorResumeNext when {
                    message.contains("Unknown Wallet Identifier") -> Completable.error(InvalidCredentialsException())
                    message.contains("locked") -> Completable.error(AccountLockedException())
                    else -> Completable.error(ServerConnectionException())
                }
            }
            return@onErrorResumeNext Completable.error(it)
        }.doOnError {
            notificationTransmitter.postEvent(NotificationEvent.PayloadUpdated)
        }
    }

    fun initializeAndDecryptFromQR(
        qrData: String,
        sessionId: String
    ): Completable {
        val qrComponents = Pairing.qRComponentsFromRawString(qrData)
        return walletApi.fetchPairingEncryptionPasswordCall(qrComponents.first).flatMapCompletable {
            val encryptionPassword = it.string()
            val encryptionPairingCode = qrComponents.second
            val guid = qrComponents.first
            val sharedKeyAndPassword = Pairing.sharedKeyAndPassword(encryptionPairingCode, encryptionPassword)
            val sharedKey = sharedKeyAndPassword[0]
            val hexEncodedPassword = sharedKeyAndPassword[1]
            val password = String(Hex.decode(hexEncodedPassword), Charsets.UTF_8)
            initializeAndDecrypt(
                sharedKey,
                guid,
                password,
                sessionId
            )
        }.onErrorResumeNext {
            Completable.error(
                ServerConnectionException(
                    (it as? HttpException)?.code().toString() + " - " +
                        (it as? HttpException)?.response()?.errorBody()?.string()
                )
            )
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
    ): Completable {
        val currentWallet = payload
        val upgradedWallet = currentWallet.upgradeV2PayloadToV3(secondPassword, defaultAccountName)
        return saveAndSync(walletBase.withWalletBody(upgradedWallet), password)
    }

    /**
     * Upgrades a V3 wallet to V4 wallet format and saves it to the server
     *
     * @param secondPassword
     * @throws Exception
     */
    fun upgradeV3PayloadToV4(secondPassword: String?): Completable {
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

        return saveAndSync(
            walletBase.withWalletBody(payload.withUpdatedBodiesAndVersion(v4WalletBodies, 4)),
            password
        )
    }

    fun updateDerivationsForAccounts(accounts: List<Account>): Completable {
        return saveAndSync(
            walletBase.withUpdatedDerivationsForAccounts(accounts),
            password
        )
    }

    fun updateAccountLabel(account: JsonSerializableAccount, label: String): Completable {
        return saveAndSync(
            walletBase.withUpdatedLabel(account, label),
            password
        )
    }

    fun updateAccountsLabels(updatedAccounts: Map<Account, String>): Completable {
        return saveAndSync(
            walletBase.withUpdatedAccountsLabel(updatedAccounts),
            password
        )
    }

    fun updateArchivedAccountState(account: JsonSerializableAccount, acrhived: Boolean): Completable {
        return saveAndSync(
            walletBase.withUpdatedAccountState(account, acrhived),
            password
        )
    }

    fun updateMnemonicVerified(verified: Boolean): Completable {
        return saveAndSync(
            walletBase.withMnemonicState(verified),
            password
        )
    }

    fun updatePassword(password: String): Completable {
        return saveAndSync(walletBase, password).doOnComplete {
            this.password = password
        }
    }

    fun updateDefaultIndex(index: Int): Completable {
        return saveAndSync(
            walletBase.withWalletBody(payload.updateDefaultIndex(index)),
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
    fun syncPubKeys(): Completable {
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
    private fun saveAndSync(newWalletBase: WalletBase, passw: String): Completable {
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

        return walletApi.updateWallet(
            guid = payload.guid,
            sharedKey = payload.sharedKey,
            activeAddressList = syncAddresses,
            encryptedPayload = payloadWrapper.toJson(),
            newChecksum = newPayloadChecksum,
            oldChecksum = oldPayloadChecksum,
            device = device.osType
        ).doOnComplete {
            val updatedWalletBase = newWalletBase.withUpdatedChecksum(newPayloadChecksum)
            updatePayload(updatedWalletBase)
        }.doFinally {
            notificationTransmitter.postEvent(NotificationEvent.PayloadUpdated)
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
    ): Single<Account> {
        val updatedWallet: Wallet = payload.addAccount(
            label,
            secondPassword
        )
        return saveAndSync(
            walletBase.withWalletBody(updatedWallet),
            password
        ).doOnComplete {
            updateAllBalances()
        }.thenSingle {
            Single.just(walletBase.wallet.walletBody!!.lastCreatedAccount())
        }
    }

    /**
     * Inserts a [ImportedAddress] into the user's [Wallet] and then syncs the wallet with
     * the server.
     *
     * @param importedAddress The [ImportedAddress] to be added
     * @throws Exception Possible if saving the Wallet fails
     */
    fun addImportedAddress(importedAddress: ImportedAddress?): Completable {
        val wallet: Wallet = payload.addImportedAddress(importedAddress)
        val updatedWalletBase = walletBase.withWalletBody(wallet)
        return saveAndSync(
            updatedWalletBase,
            password
        ).doOnComplete {
            updateAllBalances()
        }
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
    ): Single<ImportedAddress> {
        return try {
            val address = payload.updateKeyForImportedAddress(
                key,
                secondPassword
            )
            saveAndSync(
                walletBase.withWalletBody(payload.replaceOrAddImportedAddress(address)),
                password
            ).thenSingle {
                Single.just(address)
            }
        } catch (e: NoSuchAddressException) {
            addImportedAddressFromKey(key, secondPassword)
        }
    }

    private fun addImportedAddressFromKey(
        key: SigningKey,
        secondPassword: String?
    ): Single<ImportedAddress> {
        val address = payload.importedAddressFromKey(
            key,
            secondPassword,
            device.osType,
            appVersion.appVersion
        )
        val wallet: Wallet = payload.addImportedAddress(address)
        return saveAndSync(
            walletBase.withWalletBody(wallet),
            password
        ).thenSingle {
            Single.just(address)
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
                decryptedPrivateKey!!,
                importedAddress.address
            )
        )
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
    ): String {
        val hdAccount = payload.walletBody!!
            .getHDAccountFromAccountBody(account)[if (derivationType === Derivation.LEGACY_TYPE) 0 else 1]!!

        return hdAccount.receive
            .getAddressAt(
                position,
                if (derivationType === Derivation.LEGACY_TYPE) {
                    Derivation.LEGACY_PURPOSE
                } else
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

    fun getNextReceiveAddressAndReserve(account: Account, reserveLabel: String): Single<String> {
        val derivationType = derivationTypeFromXPub(account.xpubs.default)
        val nextIndex = getNextReceiveAddressIndexBtc(account)
        return reserveAddress(account, nextIndex, reserveLabel).thenSingle {
            Single.just(getReceiveAddress(account, nextIndex, derivationType))
        }
    }

    private fun reserveAddress(account: Account, index: Int, label: String?): Completable {
        val updatedAccount = account.addAddressLabel(index, label!!)
        return saveAndSync(
            walletBase.withWalletBody(payload.updateAccount(account, updatedAccount)),
            password
        )
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

    fun updateNotesForTxHash(transactionHash: String, notes: String): Completable {
        return saveAndSync(
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
