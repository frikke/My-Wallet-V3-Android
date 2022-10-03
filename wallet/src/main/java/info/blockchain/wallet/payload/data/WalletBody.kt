package info.blockchain.wallet.payload.data

import com.blockchain.api.services.NonCustodialBitcoinService
import com.blockchain.extensions.replace
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import info.blockchain.wallet.bip44.HDAccount
import info.blockchain.wallet.bip44.HDWallet
import info.blockchain.wallet.bip44.HDWalletFactory
import info.blockchain.wallet.dynamicselfcustody.CoinConfiguration
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.keys.MasterKey
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.payload.HDWalletsContainer
import info.blockchain.wallet.payload.data.Derivation.Companion.SEGWIT_BECH32_TYPE
import info.blockchain.wallet.payload.model.toBalanceMap
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import info.blockchain.wallet.util.DoubleEncryptionFactory
import info.blockchain.wallet.util.PrivateKeyFactory
import java.util.LinkedList
import org.spongycastle.util.encoders.Hex

class WalletBody(
    private val version: Int,
    val walletBodyDto: WalletBodyDto,
    private val HD: HDWalletsContainer
) {

    init {
        if (HD.isInstantiated.not()) {
            instantiateBip44Wallet()
        }
    }

    val mnemonicVerified: Boolean
        get() = walletBodyDto.mnemonicVerified

    fun decryptHDWallet(
        validatedSecondPassword: String?,
        sharedKey: String,
        iterations: Int
    ) {

        if (validatedSecondPassword != null && !HD.isDecrypted) {
            val encryptedSeedHex = walletBodyDto.seedHex
            val decryptedSeedHex = DoubleEncryptionFactory.decrypt(
                encryptedSeedHex,
                sharedKey,
                validatedSecondPassword,
                iterations
            )
            HD.restoreWallets(
                HDWalletFactory.Language.US,
                decryptedSeedHex,
                walletBodyDto.passphrase,
                walletBodyDto.accounts.size
            )
        }
    }

    val seedHex: String
        get() = walletBodyDto.seedHex

    val accounts: List<Account>
        get() = walletBodyDto.accounts

    private fun instantiateBip44Wallet() {
        try {
            val walletSize = walletBodyDto.accounts.takeIf { it.isNotEmpty() }?.size ?: DEFAULT_NEW_WALLET_SIZE
            HD.restoreWallets(
                HDWalletFactory.Language.US,
                walletBodyDto.seedHex,
                walletBodyDto.passphrase,
                walletSize
            )
        } catch (e: Exception) {
            HD.restoreWatchOnly(walletBodyDto.accounts)
        }

        if (!HD.isInstantiated) {
            throw HDWalletException("HD instantiation failed")
        }
    }

    private fun validateHD() {
        if (!HD.isInstantiated) {
            throw HDWalletException("HD wallet not instantiated")
        } else if (!HD.isDecrypted) {
            throw HDWalletException("Wallet private key unavailable. First decrypt with second password.")
        }
    }

    val defaultAccountIdx: Int
        get() = walletBodyDto.defaultAccountIdx ?: MISSING_DEFAULT_INDEX_VALUE

    fun getAccount(accountId: Int) = walletBodyDto.accounts[accountId]

    fun upgradeAccountsToV4(secondPassword: String?, sharedKey: String, iterations: Int): WalletBody {

        val upgradedAccounts = walletBodyDto.accounts.mapIndexed { index, account ->
            val legacyHdAccount = HD.getLegacyAccount(index)
            val segWitHdAccount = HD.getSegwitAccount(index)!!
            val accountV3 = account as? AccountV3 ?: return@mapIndexed account

            AccountV4(
                label = account.label,
                _defaultType = SEGWIT_BECH32_TYPE,
                _isArchived = account.isArchived,
                derivations = listOf(
                    Derivation(
                        type = Derivation.LEGACY_TYPE,
                        purpose = Derivation.LEGACY_PURPOSE,
                        xpriv = accountV3.xpriv,
                        xpub = accountV3.legacyXpub,
                        cache = AddressCache.setCachedXPubs(legacyHdAccount),
                        _addressLabels = account.addressLabels,
                    ),
                    segwitDerivation(segWitHdAccount)
                )
            ).encryptAccountIfNeeded(secondPassword, sharedKey, iterations)
        }
        return WalletBody(
            version = WalletWrapper.V4,
            HD = HD,
            walletBodyDto = walletBodyDto.copy(accounts = upgradedAccounts)
        )
    }

    private fun Account.encryptAccountIfNeeded(secondPassword: String?, sharedKey: String, iterations: Int): Account {
        return if (secondPassword != null) {
            val encryptedPrivateKey = DoubleEncryptionFactory.encrypt(
                xpriv,
                sharedKey,
                secondPassword,
                iterations
            )
            this.withEncryptedPrivateKey(encryptedPrivateKey)
        } else {
            this
        }
    }

    fun updateDefaultindex(index: Int): WalletBody {
        return WalletBody(
            version = version,
            HD = HD,
            walletBodyDto = walletBodyDto.copy(defaultAccountIdx = index)
        )
    }

    fun updateDerivationsForAccounts(accounts: List<Account>): WalletBody {
        var mWalletBodyDto = walletBodyDto
        accounts.forEach { account ->
            val index = accounts.indexOf(account)
            val legacyAccount = HD.getLegacyAccount(index)
            val segWit = HD.getSegwitAccount(index)
            val derivations: List<Derivation> = getDerivations(legacyAccount, segWit!!)
            mWalletBodyDto = mWalletBodyDto.withUpdatedAccounts(
                accounts.replace(
                    account,
                    AccountV4(
                        label = account.label,
                        _defaultType = SEGWIT_BECH32_TYPE,
                        _isArchived = account.isArchived,
                        derivations = derivations
                    )
                )
            )
        }

        return WalletBody(
            version = version,
            walletBodyDto = mWalletBodyDto,
            HD = HD
        )
    }

    fun updateDefaultDerivationTypeForAccounts(accounts: List<Account>): WalletBody {
        var mWalletBodyDto = walletBodyDto
        accounts.forEach { account ->
            val index = accounts.indexOf(account)
            val legacyAccount = HD.getLegacyAccount(index)
            val segWit = HD.getSegwitAccount(index)
            val derivations: List<Derivation> = getDerivations(legacyAccount, segWit!!)
            mWalletBodyDto = mWalletBodyDto.withUpdatedAccounts(
                accounts.replace(
                    account,
                    AccountV4(
                        label = account.label,
                        _defaultType = SEGWIT_BECH32_TYPE,
                        _isArchived = account.isArchived,
                        derivations = derivations
                    )
                )
            )
        }

        return WalletBody(
            version = version,
            walletBodyDto = mWalletBodyDto,
            HD = HD
        )
    }

    private fun segwitDerivation(segWit: HDAccount): Derivation {
        validateHD()
        return Derivation.createSegwit(
            segWit.xPriv,
            segWit.xpub,
            AddressCache.setCachedXPubs(segWit)
        )
    }

    /**
     * @return Non-archived account xpubs
     */
    fun getActiveXpubs(): List<XPubs> {
        return walletBodyDto.accounts.mapNotNull {
            it.takeIf { !it.isArchived }?.xpubs
        }
    }

    fun withNewAccount(label: String, secondPassword: String?, sharedKey: String, iterations: Int): WalletBody {
        validateHD()
        val index = HD.addAccount()
        val legacyAccount = HD.getLegacyAccount(index)
        val segwitAccount = HD.getSegwitAccount(index)

        /**
         * Hardcoding v4. We dont support earlier versions
         */
        val account = createAccount(WalletWrapper.V4, label, legacyAccount, segwitAccount!!).encryptIfNeeded(
            secondPassword, sharedKey, iterations
        )
        return WalletBody(
            version = version,
            walletBodyDto = walletBodyDto.withUpdatedAccounts(walletBodyDto.accounts.plus(account)),
            HD = HD
        )
    }

    private fun Account.encryptIfNeeded(secondPassword: String?, sharedKey: String, iterations: Int): Account =
        secondPassword?.let {
            val encryptedPrivateKey = DoubleEncryptionFactory.encrypt(
                xpriv,
                sharedKey,
                secondPassword,
                iterations
            )
            return this.withEncryptedPrivateKey(encryptedPrivateKey)
        } ?: this

    fun getHDKeysForSigning(
        account: Account,
        unspentOutputBundle: SpendableUnspentOutputs
    ): List<SigningKey> {
        validateHD()
        val keys = mutableListOf<SigningKey>()
        val hdAccounts = getHDAccountFromAccountBody(account)

        unspentOutputBundle.spendableOutputs.forEach { utxo ->
            val hdAccount = hdAccounts[
                if (utxo.isSegwit) 1 else 0
            ]
            if (hdAccount != null && utxo.xpub != null) {
                val split = utxo.xpub.derivationPath.split("/")
                val chain = Integer.parseInt(split[1])
                val addressIndex = Integer.parseInt(split[2])
                val hdAddress = hdAccount.getChain(chain)
                    .getAddressAt(addressIndex, Derivation.SEGWIT_BECH32_PURPOSE)
                val walletKey = PrivateKeyFactory().getSigningKey(
                    PrivateKeyFactory.WIF_COMPRESSED,
                    hdAddress.privateKeyString
                )
                keys.add(walletKey)
            }
        }
        return keys
    }

    fun getHDAccountFromAccountBody(accountBody: Account): List<HDAccount?> {
        if (!HD.isInstantiated) {
            throw HDWalletException("HD wallet not instantiated")
        }

        var legacyAccount: HDAccount? = null
        var segwitAccount: HDAccount? = null
        HD.legacyAccounts?.forEach { account ->
            if (account.xpub == accountBody.xpubForDerivation(Derivation.LEGACY_TYPE)) {
                legacyAccount = account
            }
        }

        HD.segwitAccounts?.forEach { account ->
            if (account.xpub == accountBody.xpubForDerivation(SEGWIT_BECH32_TYPE)) {
                segwitAccount = account
            }
        }
        return listOf(legacyAccount, segwitAccount)
    }

    // no need for second pw. only using HD xpubs
    // TODO: 16/02/2017 Old. Investigate better way to do this

    fun getXpubToAccountIndexMap(): BiMap<String, Int> {
        if (!HD.isInstantiated) {
            throw HDWalletException("HD wallet not instantiated")
        }
        val xpubToAccountIndexMap = HashBiMap.create<String, Int>()
        val accountList = HD.legacyAccounts
        for (account in accountList!!) {
            xpubToAccountIndexMap[account.xpub] = account.id
        }
        return xpubToAccountIndexMap
    }

    /**
     * Bip44 master private key. Not to be confused with bci HDWallet seed
     */
    fun getMasterKey(): MasterKey? {
        validateHD()
        return HD.masterKey
    }

    fun getHdSeed(): ByteArray? {
        validateHD()
        return HD.hdSeed
    }

    fun getMnemonic(): List<String>? {
        validateHD()
        return HD.mnemonic
    }

    fun getLabelFromXpub(xpub: String): String? {
        walletBodyDto.accounts.forEach { account ->
            if (account.containsXpub(xpub)) {
                return account.label
            }
        }
        return null
    }

    fun getDynamicHdAccount(coinConfiguration: CoinConfiguration) = HD.getDynamicAccount(coinConfiguration)

    fun updateSeedHex(doubleEncryptedSeedHex: String): WalletBody = WalletBody(
        version = version,
        walletBodyDto = walletBodyDto.copy(
            seedHex = doubleEncryptedSeedHex
        ),
        HD = HD
    )

    /**
     * Returns a new WalletBody with replaced an account,
     * with the the one that has encypted keys with second passw
     */
    fun replaceAccount(oldAccount: Account, newAccount: Account): WalletBody = WalletBody(
        version = version,
        walletBodyDto = walletBodyDto.copy(
            accounts = walletBodyDto.accounts.replace(
                oldAccount,
                newAccount
            )
        ),
        HD = HD
    )

    fun lastCreatedAccount(): Account = walletBodyDto.accounts.last()
    fun updateAccountLabel(account: Account, label: String): WalletBody {
        return WalletBody(
            version,
            walletBodyDto.updateAccountLabel(account, label),
            HD
        )
    }

    fun updateAccountState(account: Account, isArchived: Boolean): WalletBody =
        WalletBody(
            version,
            walletBodyDto.updateAccountArchivedState(account, isArchived),
            HD
        )

    fun updateMnemonicState(verified: Boolean): WalletBody = WalletBody(
        version,
        walletBodyDto.copy(
            _mnemonicVerified = verified
        ),
        HD
    )

    companion object {
        private const val DEFAULT_MNEMONIC_LENGTH = 12
        private const val DEFAULT_NEW_WALLET_SIZE = 1
        private const val DEFAULT_PASSPHRASE = ""

        fun create(defaultAccountName: String, createV4: Boolean = true): WalletBody {
            val hd = HDWalletsContainer()
            hd.createWallets(
                HDWalletFactory.Language.US,
                DEFAULT_MNEMONIC_LENGTH,
                DEFAULT_PASSPHRASE,
                DEFAULT_NEW_WALLET_SIZE
            )
            val wrapperVersion = if (createV4) WalletWrapper.V4 else WalletWrapper.V3
            val hdAccounts = hd.legacyAccounts
            val accounts = hdAccounts?.mapIndexed { index, _ ->
                var label = defaultAccountName
                if (index > 0) {
                    label = defaultAccountName + " " + (index + 1)
                }
                if (createV4) {
                    createAccount(
                        wrapperVersion,
                        label,
                        hd.getLegacyAccount(index),
                        hd.getSegwitAccount(index)
                    )
                } else
                    createAccount(
                        wrapperVersion,
                        label,
                        hd.getLegacyAccount(index),
                        null
                    )
            } ?: emptyList()

            return WalletBody(
                walletBodyDto = WalletBodyDto(
                    accounts = accounts,
                    defaultAccountIdx = 0,
                    _mnemonicVerified = false,
                    seedHex = hd.seedHex,
                    passphrase = DEFAULT_PASSPHRASE
                ),
                HD = HDWalletsContainer(),
                version = if (createV4) WalletWrapper.V4 else WalletWrapper.V3
            )
        }

        private fun createAccount(
            version: Int,
            label: String,
            legacyAccount: HDAccount,
            segWit: HDAccount?
        ): Account {
            val accountBody: Account
            if (version == WalletWrapper.V4) {
                val derivations = getDerivations(legacyAccount = legacyAccount, segWit = segWit!!)
                accountBody = AccountV4(
                    label = label,
                    _defaultType = SEGWIT_BECH32_TYPE,
                    _isArchived = false,
                    derivations = derivations
                )
            } else {
                accountBody = AccountV3(
                    label = label,
                    isArchived = false,
                    xpriv = legacyAccount.xPriv,
                    legacyXpub = legacyAccount.xpub,
                    _addressLabels = emptyList(),
                    addressCache = AddressCache.setCachedXPubs(legacyAccount)
                )
            }
            return accountBody
        }

        private fun getDerivations(legacyAccount: HDAccount, segWit: HDAccount): List<Derivation> =
            listOf(
                Derivation.create(
                    legacyAccount.xPriv,
                    legacyAccount.xpub,
                    AddressCache.setCachedXPubs(legacyAccount)
                ),
                Derivation.createSegwit(
                    segWit.xPriv,
                    segWit.xpub,
                    AddressCache.setCachedXPubs(segWit)
                )
            )

        fun recoverFromMnemonic(
            mnemonic: String,
            defaultAccountName: String,
            bitcoinApi: NonCustodialBitcoinService
        ): WalletBody = recoverFromMnemonic(
            mnemonic = mnemonic,
            passphrase = "",
            defaultAccountName = defaultAccountName,
            _walletSize = 0,
            bitcoinApi = bitcoinApi
        )

        /**
         * Only from tests?
         */
        fun recoverFromMnemonic(
            mnemonic: String,
            passphrase: String,
            defaultAccountName: String,
            bitcoinApi: NonCustodialBitcoinService
        ): WalletBody = recoverFromMnemonic(
            mnemonic = mnemonic,
            passphrase = passphrase,
            defaultAccountName = defaultAccountName,
            _walletSize = 0,
            bitcoinApi = bitcoinApi
        )

        private fun recoverFromMnemonic(
            mnemonic: String,
            passphrase: String,
            defaultAccountName: String,
            _walletSize: Int,
            bitcoinApi: NonCustodialBitcoinService
        ): WalletBody {
            val wrapperVersion = WalletWrapper.V4
            val HD = HDWalletsContainer()
            // Start with initial wallet size of 1.
            // After wallet is recovered we'll check how many accounts to restore
            HD.restoreWallets(
                HDWalletFactory.Language.US,
                mnemonic,
                passphrase,
                DEFAULT_NEW_WALLET_SIZE
            )

            var walletSize = _walletSize

            if (walletSize <= 0) {
                val legacyWalletSize = getDeterminedSize(
                    _walletSize = 1,
                    trySize = 5,
                    _currentGap = 0,
                    bitcoinApi = bitcoinApi,
                    bip44Wallet = HD.getHDWallet(Derivation.LEGACY_PURPOSE),
                    purpose = Derivation.LEGACY_PURPOSE
                )
                var segwitP2SHWalletSize = 0
                if (wrapperVersion == WalletWrapper.V4) {
                    segwitP2SHWalletSize = getDeterminedSize(
                        _walletSize = 1,
                        trySize = 5,
                        _currentGap = 0,
                        bitcoinApi = bitcoinApi,
                        bip44Wallet = HD.getHDWallet(Derivation.SEGWIT_BECH32_PURPOSE),
                        purpose = Derivation.SEGWIT_BECH32_PURPOSE
                    )
                }
                walletSize = legacyWalletSize.coerceAtLeast(segwitP2SHWalletSize)
            }
            HD.restoreWallets(
                HDWalletFactory.Language.US,
                mnemonic,
                passphrase,
                walletSize
            )

            val legacyAccounts = HD.legacyAccounts
            val segwitAccounts = HD.segwitAccounts

            val recoveredAccounts = legacyAccounts!!.mapIndexed { index, legacyAccount ->
                val label = if (index > 0) {
                    defaultAccountName + " " + (index + 1)
                } else defaultAccountName

                val segwitAccount = segwitAccounts!![index]
                createAccount(
                    version = wrapperVersion,
                    label = label,
                    legacyAccount = legacyAccount,
                    segWit = segwitAccount
                )
            }

            return WalletBody(
                walletBodyDto = WalletBodyDto(
                    seedHex = Hex.toHexString(HD.seed),
                    passphrase = HD.passphrase!!,
                    _mnemonicVerified = false,
                    accounts = recoveredAccounts,
                    defaultAccountIdx = 0
                ),
                HD = HD,
                version = wrapperVersion
            )
        }

        private fun getDeterminedSize(
            _walletSize: Int,
            trySize: Int,
            _currentGap: Int,
            bitcoinApi: NonCustodialBitcoinService,
            bip44Wallet: HDWallet?,
            purpose: Int
        ): Int {

            var walletSize = _walletSize
            var currentGap = _currentGap

            val xPubs = LinkedList<String>()
            for (i in 0 until trySize) {
                val account = bip44Wallet!!.addAccount()
                xPubs.add(account.xpub)
            }

            val exe = bitcoinApi.getBalance(
                coin = NonCustodialBitcoinService.BITCOIN,
                addressAndXpubListLegacy = if (purpose == Derivation.LEGACY_PURPOSE) xPubs else emptyList(),
                xpubListBech32 = if (purpose == Derivation.SEGWIT_BECH32_PURPOSE) xPubs else emptyList(),
                filter = NonCustodialBitcoinService.BalanceFilter.Confirmed
            ).execute()

            if (!exe.isSuccessful) {
                throw Exception("${exe.code()} ${exe.errorBody()}")
            }
            val map = exe.body()!!.toBalanceMap()

            val lookAheadTotal = 10
            xPubs.forEach { xpub ->
                // If account has txs
                if (map[xpub]!!.txCount > 0) {
                    walletSize += 1
                    currentGap = 0
                } else {
                    currentGap += 1
                }
                if (currentGap >= lookAheadTotal) {
                    return walletSize
                }
            }
            return getDeterminedSize(
                _walletSize = walletSize,
                trySize = trySize * 2,
                _currentGap = currentGap,
                bitcoinApi = bitcoinApi,
                bip44Wallet = bip44Wallet,
                purpose = purpose
            )
        }

        const val HD_DEFAULT_WALLET_INDEX = 0
    }
}
