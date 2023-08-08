package com.blockchain.core.payload

import com.blockchain.annotations.MoveCandidate
import com.blockchain.api.services.NonCustodialBitcoinService
import com.blockchain.api.services.SelfCustodyServiceAuthCredentials
import com.blockchain.core.utils.RefreshUpdater
import com.blockchain.core.utils.schedulers.applySchedulers
import com.blockchain.domain.wallet.CoinType
import com.blockchain.logging.RemoteLogger
import com.blockchain.rx.MainScheduler
import com.blockchain.serialization.JsonSerializableAccount
import com.blockchain.utils.then
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.bip44.HDWalletFactory
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.keys.MasterKey
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.WalletPayloadService
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.AccountV3
import info.blockchain.wallet.payload.data.AccountV4
import info.blockchain.wallet.payload.data.Derivation
import info.blockchain.wallet.payload.data.ImportedAddress
import info.blockchain.wallet.payload.data.MISSING_DEFAULT_INDEX_VALUE
import info.blockchain.wallet.payload.data.Wallet
import info.blockchain.wallet.payload.data.WalletBody.Companion.HD_DEFAULT_WALLET_INDEX
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.model.Balance
import info.blockchain.wallet.payment.OutputType
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import info.blockchain.wallet.util.PrivateKeyFactory
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.math.BigInteger
import org.bitcoinj.core.AddressFormatException
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.SegwitAddress
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.Script
import org.spongycastle.util.encoders.Hex

class WalletUpgradeFailure(
    msg: String,
    cause: Throwable? = null
) : Exception(
    "$msg (cause: ${cause?.message})",
    cause
)

class PayloadDataManager internal constructor(
    private val payloadService: PayloadService,
    private val bitcoinApi: NonCustodialBitcoinService,
    @MoveCandidate("Move this down to the PayloadManager layer, with the other crypto tools")
    private val privateKeyFactory: PrivateKeyFactory,
    private val payloadManager: PayloadManager,
    private val remoteLogger: RemoteLogger
) : WalletPayloadService, SelfCustodyServiceAuthCredentials {

    override val password: String
        get() = tempPassword

    // /////////////////////////////////////////////////////////////////////////
    // CONVENIENCE METHODS AND PROPERTIES
    // /////////////////////////////////////////////////////////////////////////

    val accounts: List<Account>
        get() = wallet.walletBody?.accounts ?: emptyList()

    val accountCount: Int
        get() = wallet.walletBody?.accounts?.size ?: 0

    val importedAddresses: List<ImportedAddress>
        get() = wallet.importedAddressList?.filter { !it.isWatchOnly() } ?: emptyList()

    val importedAddressStringList: List<String>
        get() = wallet.importedAddressStringList ?: emptyList()

    val wallet: Wallet
        get() = payloadManager.payload

    val defaultAccountIndex: Int
        get() = wallet.walletBody?.defaultAccountIdx ?: 0

    val defaultAccount: Account
        get() = wallet.walletBody?.getAccount(defaultAccountIndex) ?: throw NoSuchElementException()

    val payloadChecksum: String?
        get() = payloadManager.payloadChecksum

    val tempPassword: String
        get() = payloadManager.tempPassword

    val importedAddressesBalance: BigInteger
        get() = payloadManager.importedAddressesBalance

    override val isDoubleEncrypted: Boolean
        get() = wallet.isDoubleEncryption

    override val initialised: Boolean
        get() = payloadManager.initialised()

    override val isBackedUp: Boolean
        get() = payloadManager.isWalletBackedUp

    val mnemonic: List<String>
        get() = payloadManager.payload.walletBody?.getMnemonic() ?: throw NoSuchElementException()

    override val guid: String
        get() = wallet.guid

    override val guidOrNull: String?
        get() = try {
            guid
        } catch (ex: UninitializedPropertyAccessException) {
            null
        }

    private val hashedSharedKey: String
        get() = String(Hex.encode(Sha256Hash.hash(sharedKey.toByteArray())))

    override val hashedSharedKeyOrNull: String?
        get() = try {
            hashedSharedKey
        } catch (ex: UninitializedPropertyAccessException) {
            null
        }

    private val hashedGuid: String
        get() = String(Hex.encode(Sha256Hash.hash(guid.toByteArray())))

    override val hashedGuidOrNull: String?
        get() = try {
            hashedGuid
        } catch (ex: UninitializedPropertyAccessException) {
            null
        }

    override val sharedKey: String
        get() = wallet.sharedKey

    override val masterKey: MasterKey
        get() = payloadManager.masterKey()

    val isWalletUpgradeRequired: Boolean
        get() = payloadManager.isV3UpgradeRequired() || payloadManager.isV4UpgradeRequired()

    // /////////////////////////////////////////////////////////////////////////
    // AUTH METHODS
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Decrypts and initializes a wallet from a payload String. Handles both V3 and V1 wallets. Will
     * return a [DecryptionException] if the password is incorrect, otherwise can return a
     * [HDWalletException] which should be regarded as fatal.
     *
     * @param payload The payload String to be decrypted
     * @param password The user's password
     * @return A [Completable] object
     */
    fun initializeFromPayload(payload: String, password: String): Completable =
        payloadService.initializeFromPayload(payload, password).doOnError {
            remoteLogger.logException(it)
        }.applySchedulers()

    /**
     * Restores a HD wallet from a 12 word mnemonic and initializes the [PayloadDataManager].
     * Also creates a new Blockchain.info account in the process.
     *
     * @param mnemonic The 12 word mnemonic supplied as a String of words separated by whitespace
     * @param walletName The name of the wallet, usually a default name localised by region
     * @param email The user's email address, preferably not associated with another account
     * @param password The user's choice of password
     * @return An [Observable] wrapping a [Wallet] object
     */
    fun restoreHdWallet(
        mnemonic: String,
        walletName: String,
        email: String,
        password: String
    ): Single<Wallet> = payloadService.restoreHdWallet(
        mnemonic,
        walletName,
        email,
        password
    ).applySchedulers()

    /**
     * Retrieves a  master key from a 12 word mnemonic
     */
    fun generateMasterKeyFromSeed(
        recoveryPhrase: String
    ): MasterKey = HDWalletFactory.restoreWallet(
        language = HDWalletFactory.Language.US,
        data = recoveryPhrase,
        passphrase = "",
        nbAccounts = 1,
        // masterKey is independent from the derivation purpose
        purpose = Derivation.SEGWIT_BECH32_PURPOSE
    ).masterKey

    /**
     * Creates a new HD wallet and Blockchain.info account.
     *
     * @param password The user's choice of password
     * @param walletName The name of the wallet, usually a default name localised by region
     * @param email The user's email address, preferably not associated with another account
     * @param recaptchaToken The generated token during registration
     * @return An [Observable] wrapping a [Wallet] object
     */
    fun createHdWallet(
        password: String,
        walletName: String,
        email: String,
        recaptchaToken: String?
    ): Single<Wallet> = payloadService.createHdWallet(
        password = password,
        walletName = walletName,
        email = email,
        recaptchaToken = recaptchaToken
    ).applySchedulers()

    /**
     * Fetches the user's wallet payload, and then initializes and decrypts a payload using the
     * user's  password.
     *
     * @param sharedKey The shared key as a String
     * @param guid The user's GUID
     * @param password The user's password
     * @return A [Completable] object
     */
    fun initializeAndDecrypt(sharedKey: String, guid: String, password: String): Completable =
        payloadService.initializeAndDecrypt(
            sharedKey = sharedKey,
            guid = guid,
            password = password
        )
            .then {
                logWalletStats(hasRecoveredDerivations = false)
                recoverMissingDerivations()
            }.then {
                checkForMissingDefaultDerivationOrDefaultAccountIndex()
            }
            .doOnComplete {
                logWalletStats(hasRecoveredDerivations = true)
            }.doOnError {
                remoteLogger.logException(it)
            }
            .applySchedulers()

    /**
     * In this method we check that the payload accounts are not missing the default_derivation field and the WalletBody
     * is not missing the defaultAccountIdx. If any of them are missing then we update them and we resync the payload.
     * The original issue was created when a bug introduced by kotlinx serialisation made those fields not to get
     * encode on the payload and iOS was failing due to them missing from the payload.
     */
    private fun checkForMissingDefaultDerivationOrDefaultAccountIndex(): Completable {
        val accountsMissingDefaultType = payloadManager.payload.walletBody?.accounts?.filter { account ->
            account is AccountV4 && account.defaultType.isEmpty()
        } ?: emptyList()

        val missingDefaultIndex = payloadManager.payload.walletBody?.defaultAccountIdx == MISSING_DEFAULT_INDEX_VALUE

        val updateRequired = missingDefaultIndex || accountsMissingDefaultType.isNotEmpty()
        return when {
            !updateRequired || isDoubleEncrypted -> Completable.complete()
            else -> {
                payloadManager.updateDerivationsForAccounts(accounts)
                    .then {
                        payloadManager.updateDefaultIndex(HD_DEFAULT_WALLET_INDEX)
                    }
            }
        }
    }

    /**
     * Initializes and decrypts a user's payload given valid QR code scan data.
     *
     * @param data A QR's URI for pairing
     * @return A [Completable] object
     */
    fun handleQrCode(data: String): Completable =
        payloadService.handleQrCode(data).applySchedulers()

    /**
     * Upgrades a Wallet from V2 to V3 and saves it with the server. If saving is unsuccessful or
     * some other part fails, this will propagate an Exception.
     *
     * @param secondPassword An optional second password if the user has one
     * @param defaultAccountName A required name for the default account
     * @return A [Completable] object
     */

    private fun v3Upgrade(
        secondPassword: String?,
        defaultAccountName: String
    ) = payloadManager.upgradeV2PayloadToV3(secondPassword = secondPassword, defaultAccountName = defaultAccountName)
        .doOnError {
            remoteLogger.logException(it)
        }.onErrorResumeNext {
            throw WalletUpgradeFailure("v2 -> v3 failed", it)
        }.applySchedulers()

    private fun v4Upgrade(
        secondPassword: String?
    ) = payloadManager.upgradeV3PayloadToV4(secondPassword).doOnError {
        remoteLogger.logException(it)
    }.onErrorResumeNext {
        throw WalletUpgradeFailure("v3 -> v4 failed", it)
    }.applySchedulers()

    fun upgradeWalletPayload(
        secondPassword: String?,
        defaultAccountName: String
    ): Completable {
        logWalletUpgradeStats()

        if (payloadManager.isV3UpgradeRequired()) {
            return v3Upgrade(secondPassword = secondPassword, defaultAccountName = defaultAccountName).then {
                v4Upgrade(secondPassword)
            }
        } else if (payloadManager.isV4UpgradeRequired()) {
            return v4Upgrade(secondPassword = secondPassword)
        }
        return Completable.complete()
    }

    private fun logWalletUpgradeStats() {
        payloadManager.payload.let { payload ->
            remoteLogger.logState("doubleEncrypt", payload.isDoubleEncryption.toString())
            // There should only ever be one wallet body, but there have been historical bugs, so check:
            remoteLogger.logState("body count", (payload.walletBodies?.size ?: 0).toString())
            remoteLogger.logState("account count", (payload.walletBody?.accounts?.size ?: 0).toString())
            remoteLogger.logState("imported count", payload.importedAddressList.size.toString())
        }
    }

    private fun logWalletStats(hasRecoveredDerivations: Boolean) {
        logWalletUpgradeStats()
        remoteLogger.logState("tried recovering derivations", hasRecoveredDerivations.toString())
        payloadManager.payload.let { payload ->
            remoteLogger.logState("wallet wrapper version", payload.wrapperVersion.toString())
            remoteLogger.logState("wallet has second password", isDoubleEncrypted.toString())
            payload.walletBody?.accounts?.map { account ->
                remoteLogger.logState("account is archived", account.isArchived.toString())
                val hasNullOrEmptyXPub = account.xpubs.allAddresses().find { address ->
                    address.isNullOrEmpty()
                } != null
                remoteLogger.logState("account has null or empty xpub", hasNullOrEmptyXPub.toString())
                when (account) {
                    is AccountV3 -> {
                        remoteLogger.logState("account type", "V3")
                        remoteLogger.logState(
                            "accountV3 is legacy xpub empty",
                            account.legacyXpub.isEmpty().toString()
                        )
                    }
                    is AccountV4 -> {
                        remoteLogger.logState("account type", "V4")
                        // Address labels returns empty if the derivation is null
                        remoteLogger.logState(
                            "accountV4 derivation is null",
                            account.addressLabels.isEmpty().toString()
                        )
                        remoteLogger.logState("accountV4 derivations count", account.derivations.size.toString())
                        val hasEmptyXPub = account.derivations.find {
                            it.xpub.isEmpty()
                        } != null
                        remoteLogger.logState("accountV4 has empty xpub", hasEmptyXPub.toString())
                    }
                    else -> {
                        // Do nothing
                    }
                }
            }
        }
    }

    private fun recoverMissingDerivations(): Completable {
        val expectedNumberOfDerivations = 2
        val accountsWithMissingDerivations = payloadManager.payload.walletBody?.accounts?.filter { account ->
            account is AccountV4 && account.derivations.size < expectedNumberOfDerivations
        }
        return when {
            accountsWithMissingDerivations.isNullOrEmpty() || isDoubleEncrypted -> Completable.complete()
            else -> {
                payloadManager.updateDerivationsForAccounts(accountsWithMissingDerivations)
            }
        }
    }

    /**
     * Returns a [Completable] which saves the current payload to the server whilst also
     * forcing the sync of the user's public keys. This method generates 20 addresses per Account,
     * so it should be used only when strictly necessary (for instance, after enabling
     * notifications).
     *
     * @return A [Completable] object
     */
    fun syncPayloadAndPublicKeys(): Completable =
        payloadService.syncPayloadAndPublicKeys().applySchedulers()

    // /////////////////////////////////////////////////////////////////////////
    // TRANSACTION METHODS
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Returns [Completable] which updates transactions in the PayloadManager.
     * Completable returns no value, and is used to call functions that return void but have side
     * effects.
     *
     * @return A [Completable] object
     */
    fun updateAllTransactions(): Completable =
        payloadService.updateAllTransactions().applySchedulers()

    /**
     * Returns a [Completable] which updates all balances in the PayloadManager. Completable
     * returns no value, and is used to call functions that return void but have side effects.
     *
     * @return A [Completable] object
     */
    fun updateAllBalances(): Completable =
        payloadService.updateAllBalances().applySchedulers()

    /**
     * Update notes for a specific transaction hash and then sync the payload to the server
     *
     * @param transactionHash The hash of the transaction to be updated
     * @param notes Transaction notes
     * @return A [Completable] object
     */
    fun updateTransactionNotes(transactionHash: String, notes: String): Completable =
        payloadService.updateTransactionNotes(transactionHash, notes).applySchedulers()

    // /////////////////////////////////////////////////////////////////////////
    // ACCOUNTS AND ADDRESS METHODS
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Returns a [LinkedHashMap] of [Balance] objects keyed to their Bitcoin Cash
     * addresses.
     *
     * @param xpubs A List of Bitcoin cash accounts
     * @return A [LinkedHashMap]
     */
    fun getBalanceOfBchAccounts(
        xpubs: List<XPubs>
    ): Observable<Map<String, Balance>> =
        payloadService.getBalanceOfBchAccounts(xpubs).applySchedulers()

    /**
     * Converts any address to a label.
     *
     * @param address Accepts account receive or change chain address, as well as imported address.
     * @return Either the label associated with the address, or the original address
     */
    fun addressToLabel(address: String): String = payloadManager.getLabelFromAddress(address)

    /**
     * Returns the next Receive address for a given account index.
     *
     * @param accountIndex The index of the account for which you want an address to be generated
     * @return An [Observable] wrapping the receive address
     */
    fun getNextReceiveAddress(accountIndex: Int): Observable<String> {
        val account = accounts[accountIndex]
        return getNextReceiveAddress(account)
    }

    /**
     * Returns the next Receive address for a given [Account]
     *
     * @param account The [Account] for which you want an address to be generated
     * @return An [Observable] wrapping the receive address
     */
    fun getNextReceiveAddress(account: Account): Observable<String> =
        Observable.fromCallable {
            payloadManager.getNextReceiveAddress(
                account
            )!!
        }.subscribeOn(Schedulers.computation())
            .observeOn(MainScheduler.main())

    /**
     * Allows you to generate a receive address at an arbitrary number of positions on the chain
     * from the next valid unused address. For example, the passing 5 as the position will generate
     * an address which correlates with the next available address + 5 positions.
     *
     * @param account The [Account] you wish to generate an address from
     * @param position Represents how many positions on the chain beyond what is already used that
     * you wish to generate
     * @return A bitcoin address
     */
    fun getReceiveAddressAtPosition(account: Account, position: Int): String? =
        payloadManager.getReceiveAddressAtPosition(
            account,
            position
        )

    /**
     * Returns the next Receive address for a given [Account]
     *
     * @param accountIndex The index of the account for which you want an address to be generated
     * @param label Label used to reserve address
     * @return An [Observable] wrapping the receive address
     */
    fun getNextReceiveAddressAndReserve(accountIndex: Int, label: String): Observable<String> {
        val account = accounts[accountIndex]
        return payloadManager.getNextReceiveAddressAndReserve(
            account,
            label
        ).applySchedulers().toObservable()
    }

    /**
     * Returns the next Change address for a given account index.
     *
     * @param accountIndex The index of the account for which you want an address to be generated
     * @return An [Observable] wrapping the receive address
     */
    fun getNextChangeAddress(accountIndex: Int): Observable<String> {
        val account = accounts[accountIndex]
        return getNextChangeAddress(account)
    }

    /**
     * Returns the next Change address for a given [Account].
     *
     * @param account The [Account] for which you want an address to be generated
     * @return An [Observable] wrapping the receive address
     */
    fun getNextChangeAddress(account: Account): Observable<String> =
        Observable.fromCallable {
            payloadManager.getNextChangeAddress(
                account
            )!!
        }.subscribeOn(Schedulers.computation())
            .observeOn(MainScheduler.main())

    /**
     * Returns an [SigningKey] for a given [ImportedAddress], optionally with a second password
     * should the private key be encrypted.
     *
     * @param importedAddress The [ImportedAddress] to generate an Elliptic Curve Key for
     * @param secondPassword An optional second password, necessary if the private key is encrypted
     * @return An Elliptic Curve Key object [SigningKey]
     * @see ImportedAddress.isPrivateKeyEncrypted
     */
    fun getAddressSigningKey(importedAddress: ImportedAddress, secondPassword: String?): SigningKey =
        payloadManager.getAddressSigningKey(importedAddress, secondPassword)

    /**
     * Derives new [Account] from the master seed
     *
     * @param accountLabel A label for the account
     * @param secondPassword An optional double encryption password
     * @return An [Observable] wrapping the newly created Account
     */
    fun createNewAccount(accountLabel: String, secondPassword: String?): Observable<Account> =
        payloadService.createNewAccount(accountLabel, secondPassword).applySchedulers()

    /**
     * Add a private key for a [ImportedAddress]
     *
     * @param key An [SigningKey]
     * @param secondPassword An optional double encryption password
     * @return An [Observable] representing a successful save
     */
    fun addImportedAddressFromKey(key: SigningKey, secondPassword: String?): Single<ImportedAddress> =
        payloadService.setKeyForImportedAddress(key, secondPassword).applySchedulers()
            .singleOrError()

    /**
     * Returns an Elliptic Curve key for a given private key
     *
     * @param keyFormat The format of the private key
     * @param keyData The private key from which to derive the SigningKey
     * @return An [SigningKey]
     * @see PrivateKeyFactory
     */
    fun getKeyFromImportedData(keyFormat: String, keyData: String): Single<SigningKey> =
        Single.fromCallable {
            privateKeyFactory.getKeyFromImportedData(keyFormat, keyData, bitcoinApi)
        }.applySchedulers()

    fun getBip38KeyFromImportedData(keyData: String, keyPassword: String): Single<SigningKey> =
        Single.fromCallable {
            privateKeyFactory.getBip38Key(keyData, keyPassword)
        }.applySchedulers()

    /**
     * Returns the balance of an address. If the address isn't found in the address map object, the
     * method will return CryptoValue.Zero(Btc) instead of a null object.
     *
     * @param xpub The address whose balance you wish to query
     * @return A [CryptoValue] representing the total funds in the address
     */

    fun getAddressBalance(xpub: XPubs): Money =
        payloadManager.getAddressBalance(xpub)

    private val balanceUpdater = RefreshUpdater<Money>(
        fnRefresh = { updateAllBalances() },
        refreshInterval = BALANCE_REFRESH_INTERVAL
    )

    fun getAddressBalanceRefresh(
        address: XPubs,
        forceRefresh: Boolean = false
    ): Single<Money> =
        balanceUpdater.get(
            local = { getAddressBalance(address) },
            force = forceRefresh
        )

    /**
     * Updates the balance of the address as well as that of the entire wallet. To be called after a
     * successful sweep to ensure that balances are displayed correctly before syncing the wallet.
     *
     * @param address An address from which you've just spent funds
     * @param spentAmount The spent amount as a long
     * @throws Exception Thrown if the address isn't found
     */
    fun subtractAmountFromAddressBalance(address: String, spentAmount: Long) {
        payloadManager.subtractAmountFromAddressBalance(address, BigInteger.valueOf(spentAmount))
    }

    /**
     * Increments the index on the receive chain for an [Account] object.
     *
     * @param account The [Account] you wish to increment
     */
    fun incrementReceiveAddress(account: Account) {
        payloadManager.incrementNextReceiveAddress(account)
    }

    /**
     * Increments the index on the change chain for an [Account] object.
     *
     * @param account The [Account] you wish to increment
     */
    fun incrementChangeAddress(account: Account) {
        payloadManager.incrementNextChangeAddress(account)
    }

    /**
     * Returns an xPub from an address if the address belongs to this wallet.
     *
     * @param address The address you want to query as a String
     * @return An xPub as a String
     */
    fun getXpubFromAddress(address: String): String? = payloadManager.getXpubFromAddress(address)

    /**
     * Returns true if the supplied address belongs to the user's wallet.
     *
     * @param address The address you want to query as a String
     * @return true if the address belongs to the user
     */
    fun isOwnHDAddress(address: String): Boolean = payloadManager.isOwnHDAddress(address)

    // /////////////////////////////////////////////////////////////////////////
    // CONTACTS/METADATA/IWCS/CRYPTO-MATRIX METHODS
    // /////////////////////////////////////////////////////////////////////////

    fun getAccount(accountPosition: Int): Account =
        wallet.walletBody?.getAccount(accountPosition) ?: throw NoSuchElementException()

    fun getAccountTransactions(xpub: XPubs, limit: Int, offset: Int): Single<List<TransactionSummary>> =
        Single.fromCallable {
            payloadManager.getAccountTransactions(xpub, limit, offset)
        }

    fun getDynamicHdAccount(coinType: CoinType) =
        wallet.walletBody?.getDynamicHdAccount(coinType)

    /**
     * Returns the transaction notes for a given transaction hash. May return null if not found.
     *
     * @param txHash The Tx hash
     * @return A string representing the Tx note, which can be null
     */
    fun getTransactionNotes(txHash: String): String? = payloadManager.payload.txNotes[txHash]

    /**
     * Returns a list of [SigningKey] objects for signing transactions.
     *
     * @param account The [Account] that you wish to send funds from
     * @param unspentOutputBundle A [SpendableUnspentOutputs] bundle for a given Account
     * @return A list of [SigningKey] objects
     */
    fun getHDKeysForSigning(
        account: Account,
        unspentOutputBundle: SpendableUnspentOutputs
    ): List<SigningKey> =
        wallet.walletBody?.getHDKeysForSigning(
            account,
            unspentOutputBundle
        ) ?: throw NoSuchElementException()

    // /////////////////////////////////////////////////////////////////////////
    // HELPER METHODS
    // /////////////////////////////////////////////////////////////////////////

    fun setDefaultIndex(defaultIndex: Int): Completable =
        payloadManager.updateDefaultIndex(defaultIndex)
            .applySchedulers()

    fun validateSecondPassword(secondPassword: String?): Boolean =
        payloadManager.validateSecondPassword(secondPassword)

    fun decryptHDWallet(secondPassword: String?) {
        payloadManager.payload.decryptHDWallet(secondPassword)
    }

    fun getXpubFormatOutputType(format: XPub.Format): OutputType {
        return when (format == XPub.Format.SEGWIT) {
            true -> OutputType.P2WPKH
            else -> OutputType.P2PKH
        }
    }

    fun getAddressOutputType(address: String): OutputType {
        val networkParam = MainNetParams.get()

        // Fallback to legacy type for fee calculation
        return getSegwitOutputTypeFromAddress(address, networkParam)
            ?: getLegacyOutputTypeFromAddress(address, networkParam)
            ?: OutputType.P2PKH
    }

    private fun getSegwitOutputTypeFromAddress(address: String, networkParam: NetworkParameters): OutputType? {
        return try {
            // `SegwitAddress.getOutputScriptType()` returns either P2WPKH or P2WSH
            val segwitAddress = SegwitAddress.fromBech32(networkParam, address)
            when (segwitAddress.outputScriptType == Script.ScriptType.P2WSH) {
                true -> OutputType.P2WSH
                else -> OutputType.P2WPKH
            }
        } catch (ignored: AddressFormatException) {
            null
        }
    }

    private fun getLegacyOutputTypeFromAddress(address: String, networkParam: NetworkParameters): OutputType? {
        return try {
            val legacyAddress = LegacyAddress.fromBase58(networkParam, address)
            when (legacyAddress.p2sh) {
                true -> OutputType.P2SH
                else -> OutputType.P2PKH
            }
        } catch (ignored: AddressFormatException) {
            null
        }
    }

    fun addAccountWithLabel(newAccountLabel: String): Single<Account> {
        return payloadManager.addAccount(newAccountLabel, null)
    }

    fun updateAccountLabel(internalAccount: JsonSerializableAccount, newLabel: String): Completable {
        return payloadManager.updateAccountLabel(internalAccount, newLabel)
            .applySchedulers()
    }

    fun updateAccountsLabel(internalAccounts: Map<Account, String>): Completable {
        return payloadManager.updateAccountsLabels(internalAccounts)
    }

    fun updateAccountArchivedState(internalAccount: JsonSerializableAccount, isArchived: Boolean): Completable {
        return payloadManager.updateArchivedAccountState(internalAccount, isArchived)
            .applySchedulers()
    }

    override fun updateMnemonicVerified(mnemonicVerified: Boolean): Completable {
        return payloadManager.updateMnemonicVerified(mnemonicVerified)
            .applySchedulers()
    }

    fun updatePassword(password: String): Completable {
        return payloadManager.updatePassword(password)
    }

    companion object {
        private const val BALANCE_REFRESH_INTERVAL = 15 * 1000L
    }
}

private fun ImportedAddress.isWatchOnly() = privateKey == null
