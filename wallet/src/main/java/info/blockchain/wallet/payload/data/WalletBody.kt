package info.blockchain.wallet.payload.data

import com.blockchain.api.services.NonCustodialBitcoinService
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import info.blockchain.wallet.bip44.HDAccount
import info.blockchain.wallet.bip44.HDWallet
import info.blockchain.wallet.bip44.HDWalletFactory
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.keys.MasterKey
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.payload.HDWalletsContainer
import info.blockchain.wallet.payload.model.toBalanceMap
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import info.blockchain.wallet.stx.STXAccount
import info.blockchain.wallet.util.DoubleEncryptionFactory
import info.blockchain.wallet.util.PrivateKeyFactory
import java.util.LinkedList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.spongycastle.util.encoders.Hex

@Serializable
class WalletBody {

    @SerialName("accounts")
    var accounts: MutableList<Account>? = null

    @SerialName("seed_hex")
    var seedHex: String? = null

    @SerialName("passphrase")
    var passphrase: String? = null

    @SerialName("mnemonic_verified")
    var mnemonicVerified = false

    @SerialName("default_account_idx")
    var defaultAccountIdx = 0

    @Transient
    var wrapperVersion = 0

    @Transient
    private val HD = HDWalletsContainer()

    fun decryptHDWallet(
        validatedSecondPassword: String?,
        sharedKey: String,
        iterations: Int
    ) {
        if (!HD.isInstantiated) {
            instantiateBip44Wallet()
        }
        if (validatedSecondPassword != null && !HD.isDecrypted) {
            val encryptedSeedHex = seedHex
            val decryptedSeedHex = DoubleEncryptionFactory.decrypt(
                encryptedSeedHex,
                sharedKey,
                validatedSecondPassword,
                iterations
            )
            HD.restoreWallets(
                HDWalletFactory.Language.US,
                decryptedSeedHex,
                passphrase!!,
                accounts!!.size
            )
        }
    }

    private fun instantiateBip44Wallet() {
        try {
            val walletSize = accounts?.size ?: DEFAULT_NEW_WALLET_SIZE
            HD.restoreWallets(
                HDWalletFactory.Language.US,
                seedHex!!,
                passphrase!!,
                walletSize
            )
        } catch (e: Exception) {
            HD.restoreWatchOnly(accounts!!)
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

    constructor()

    constructor(defaultAccountName: String, createV4: Boolean = true) {
        HD.createWallets(
            HDWalletFactory.Language.US,
            DEFAULT_MNEMONIC_LENGTH,
            DEFAULT_PASSPHRASE,
            DEFAULT_NEW_WALLET_SIZE
        )
        wrapperVersion = if (createV4) WalletWrapper.V4 else WalletWrapper.V3

        val hdAccounts = HD.legacyAccounts
        accounts = mutableListOf()

        hdAccounts?.forEachIndexed { index, _ ->
            var label = defaultAccountName
            if (index > 0) {
                label = defaultAccountName + " " + (index + 1)
            }
            if (createV4) {
                addAccount(label, HD.getLegacyAccount(index)!!, HD.getSegwitAccount(index))
            } else
                addAccount(label, HD.getLegacyAccount(index)!!, null)
        }

        seedHex = this.HD.seedHex
        defaultAccountIdx = 0
        mnemonicVerified = false
        passphrase = DEFAULT_PASSPHRASE
    }

    fun getAccount(accountId: Int) = accounts!![accountId]

    fun toJson(module: SerializersModule): String {
        val jsonBuilder = Json {
            ignoreUnknownKeys = true
            serializersModule = module
            encodeDefaults = true
        }
        return jsonBuilder.encodeToString(this)
    }

    fun upgradeAccountsToV4(): List<Account> {
        val upgradedAccounts = mutableListOf<Account>()
        wrapperVersion = WalletWrapper.V4
        accounts?.forEachIndexed { index, account ->
            val accountV4: AccountV4 = account.upgradeToV4()
            val legacyHdAccount = HD.getLegacyAccount(index)
            accountV4.derivationForType(Derivation.LEGACY_TYPE)!!.cache =
                AddressCache.setCachedXPubs(legacyHdAccount!!)
            addSegwitDerivation(accountV4, index)
            upgradedAccounts.add(accountV4)
        }
        return upgradedAccounts
    }

    fun generateDerivationsForAccount(account: Account) {
        val index = accounts!!.indexOf(account)
        val legacyAccount = HD.getLegacyAccount(index)
        val segWit = HD.getSegwitAccount(index)
        val derivations: List<Derivation> = getDerivations(legacyAccount!!, segWit!!)

        accounts!![index] = AccountV4(
            account.label,
            Derivation.SEGWIT_BECH32_TYPE,
            account.isArchived,
            derivations.toMutableList()
        )
    }

    private fun getDerivations(legacyAccount: HDAccount, segWit: HDAccount): List<Derivation> {
        val derivations = mutableListOf<Derivation>()
        val legacy = Derivation.create(
            legacyAccount.xPriv,
            legacyAccount.xpub,
            AddressCache.setCachedXPubs(legacyAccount)
        )
        val segwit = Derivation.createSegwit(
            segWit.xPriv,
            segWit.xpub,
            AddressCache.setCachedXPubs(segWit)
        )
        derivations.add(legacy)
        derivations.add(segwit)
        return derivations
    }

    private fun addSegwitDerivation(accountV4: AccountV4, accountIdx: Int) {
        validateHD()
        if (wrapperVersion != WalletWrapper.V4) {
            throw HDWalletException("HD wallet has not been upgraded to version 4")
        }
        val hdAccount = HD.getSegwitAccount(accountIdx)
        accountV4.addSegwitDerivation(hdAccount!!, accountIdx)
        accounts!!.set(accountIdx, accountV4)
    }

    /**
     * @return Non-archived account xpubs
     */
    fun getActiveXpubs(): List<XPubs> {
        return accounts?.mapNotNull {
            it.takeIf { !it.isArchived }?.xpubs
        } ?: emptyList()
    }

    fun addAccount(label: String): Account {
        validateHD()
        val index = HD.addAccount()
        val legacyAccount = HD.getLegacyAccount(index)
        val segwitAccount = HD.getSegwitAccount(index)
        return addAccount(label, legacyAccount!!, segwitAccount!!)
    }

    fun addAccount(
        label: String,
        legacyAccount: HDAccount,
        segWit: HDAccount?
    ): Account {
        val accountBody: Account
        if (wrapperVersion == WalletWrapper.V4) {
            val derivations = getDerivations(legacyAccount, segWit!!)
            accountBody = AccountV4(
                label = label,
                defaultType = Derivation.SEGWIT_BECH32_TYPE,
                isArchived = false,
                derivations = derivations.toMutableList()
            )
        } else {
            accountBody = AccountV3(
                label = label,
                isArchived = false,
                xpriv = legacyAccount.xPriv,
                legacyXpub = legacyAccount.xpub
            )
        }
        accounts!!.add(accountBody)
        return accountBody
    }

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
            if (account.xpub == accountBody.xpubForDerivation(Derivation.SEGWIT_BECH32_TYPE)) {
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
        accounts?.forEach { account ->
            if (account.containsXpub(xpub)) {
                return account.label
            }
        }
        return null
    }

    fun getSTXAccount(): STXAccount? {
        return HD.getStxAccount()
    }

    companion object {
        private const val DEFAULT_MNEMONIC_LENGTH = 12
        private const val DEFAULT_NEW_WALLET_SIZE = 1
        private const val DEFAULT_PASSPHRASE = ""

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
            val walletBody = WalletBody().apply {
                this.wrapperVersion = wrapperVersion
                this.accounts = mutableListOf()
            }

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

            legacyAccounts!!.forEachIndexed { index, legacyAccount ->
                val label = if (index > 0) {
                    defaultAccountName + " " + (index + 1)
                } else defaultAccountName

                val segwitAccount = segwitAccounts!![index]
                val account =
                    walletBody.addAccount(label = label, legacyAccount = legacyAccount, segWit = segwitAccount)
                if (wrapperVersion == WalletWrapper.V4) {
                    val accountV4 = account.upgradeToV4()
                    accountV4.addSegwitDerivation(segwitAccounts[index], index)
                    walletBody.accounts!![index] = accountV4
                }
            }

            walletBody.apply {
                this.seedHex = Hex.toHexString(HD.seed)
                this.passphrase = HD.passphrase
                this.mnemonicVerified = false
                this.defaultAccountIdx = 0
            }
            return walletBody
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

        fun fromJson(json: String, module: SerializersModule): WalletBody {
            val jsonBuilder = Json {
                ignoreUnknownKeys = true
                serializersModule = module
            }
            val walletBody: WalletBody = jsonBuilder.decodeFromString(json)
            walletBody.instantiateBip44Wallet()
            return walletBody
        }
    }
}
