package com.blockchain.coincore.btc

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.AddressResolver
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.impl.AccountRefreshTrigger
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.core.chains.bitcoin.SendDataManager
import com.blockchain.core.fees.FeeDataManager
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.FreshnessStrategy
import com.blockchain.domain.wallet.PubKeyStyle
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.serialization.JsonSerializableAccount
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet.Companion.DEFAULT_ADDRESS_DESCRIPTOR
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet.Companion.MULTIPLE_ADDRESSES_DESCRIPTOR
import com.blockchain.unifiedcryptowallet.domain.wallet.PublicKey
import com.blockchain.utils.then
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.ImportedAddress
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

/*internal*/
class BtcCryptoWalletAccount internal constructor(
    private val payloadDataManager: PayloadDataManager,
    private val sendDataManager: SendDataManager,
    private val feeDataManager: FeeDataManager,
    // Used to lookup the account in payloadDataManager to fetch receive address
    private val hdAccountIndex: Int,
    override val exchangeRates: ExchangeRatesDataManager,
    private val internalAccount: JsonSerializableAccount,
    val isHDAccount: Boolean,
    private val walletPreferences: WalletStatusPrefs,
    private val refreshTrigger: AccountRefreshTrigger,
    override val addressResolver: AddressResolver
) : CryptoNonCustodialAccount(
    CryptoCurrency.BTC
) {

    override val isImported: Boolean
        get() = internalAccount is ImportedAddress

    override val label: String
        get() = internalAccount.label

    override val isArchived: Boolean
        get() = internalAccount.isArchived

    override val isDefault: Boolean
        get() = isHDAccount && payloadDataManager.defaultAccountIndex == hdAccountIndex

    override fun balanceRx(freshnessStrategy: FreshnessStrategy): Observable<AccountBalance> =
        if (internalAccount is ImportedAddress) {
            Observable.combineLatest(
                getOnChainBalance(),
                exchangeRates.exchangeRateToUserFiat(currency)
            ) { balance, rate ->
                AccountBalance(
                    total = balance,
                    withdrawable = balance,
                    pending = Money.zero(currency),
                    dashboardDisplay = balance,
                    exchangeRate = rate
                )
            }
        } else {
            super.balanceRx(freshnessStrategy)
        }

    override val receiveAddress: Single<ReceiveAddress>
        get() = when (internalAccount) {
            is Account -> {
                payloadDataManager.getNextReceiveAddress(
                    internalAccount
                ).singleOrError()
                    .map {
                        BtcAddress(address = it, label = label)
                    }
            }
            else -> Single.error(IllegalStateException("Cannot receive to Imported Account"))
        }

    private fun getOnChainBalance(): Observable<Money> =
        getAccountBalance()
            .toObservable()

    private fun getAccountBalance(): Single<Money> =
        payloadDataManager.getAddressBalanceRefresh(xpubs, false)
            .map { it }

    override suspend fun publicKey(): List<PublicKey> {
        val segwitXpub = xpubs.forDerivation(XPub.Format.SEGWIT)
        val legacyXpub = xpubs.forDerivation(XPub.Format.LEGACY)
        return listOfNotNull(
            segwitXpub?.let { xpub ->
                PublicKey(
                    address = xpub.address,
                    descriptor = MULTIPLE_ADDRESSES_DESCRIPTOR,
                    style = PubKeyStyle.EXTENDED
                )
            },
            legacyXpub?.let { xpub ->
                PublicKey(
                    address = xpub.address,
                    descriptor = DEFAULT_ADDRESS_DESCRIPTOR,
                    style = PubKeyStyle.EXTENDED
                )
            }
        )
    }

    override val index: Int
        get() = hdAccountIndex

    override val pubKeyDescriptor
        get() = BTC_PUBKEY_DESCRIPTOR

    override fun createTxEngine(target: TransactionTarget, action: AssetAction): TxEngine =
        BtcOnChainTxEngine(
            btcDataManager = payloadDataManager,
            sendDataManager = sendDataManager,
            feeManager = feeDataManager,
            requireSecondPassword = payloadDataManager.isDoubleEncrypted,
            walletPreferences = walletPreferences,
            resolvedAddress = addressResolver.getReceiveAddress(currency, target, action)
        )

    override val stateAwareActions: Single<Set<StateAwareAction>>
        get() = super.stateAwareActions.map { actions ->
            if (!isHDAccount) {
                actions.toMutableSet().apply {
                    removeIf {
                        it.action == AssetAction.Receive
                    }
                }.toSet()
            } else {
                actions
            }
        }

    override fun updateLabel(newLabel: String): Completable {
        require(newLabel.isNotEmpty())
        return payloadDataManager.updateAccountLabel(internalAccount, newLabel).doOnComplete {
            forceRefresh()
        }
    }

    override fun archive(): Completable {
        require(!isArchived)
        require(!isDefault)
        return toggleArchived()
    }

    override fun unarchive(): Completable {
        require(isArchived)
        return toggleArchived()
    }

    private fun toggleArchived(): Completable {
        val isArchived = this.isArchived

        return updateArchivedState(!isArchived)
            .then { balanceRx().firstOrError().onErrorComplete().ignoreElement() }
            .doOnComplete { forceRefresh() }
    }

    private fun updateArchivedState(newIsArchived: Boolean): Completable {
        return payloadDataManager.updateAccountArchivedState(internalAccount, newIsArchived)
    }

    override fun setAsDefault(): Completable {
        require(!isDefault)
        require(isHDAccount)

        return payloadDataManager.setDefaultIndex(hdAccountIndex)
            .doOnComplete { forceRefresh() }
    }

    override val xpubAddress: String
        get() = when (internalAccount) {
            is Account -> internalAccount.xpubs.default.address
            is ImportedAddress -> internalAccount.address
            else -> throw java.lang.IllegalStateException("Unknown wallet type")
        }

    override val hasStaticAddress: Boolean = false

    val xpubs: XPubs
        get() = when (internalAccount) {
            is Account -> internalAccount.xpubs
            is ImportedAddress -> internalAccount.xpubs()
            else -> throw java.lang.IllegalStateException("Unknown wallet type")
        }

    fun getSigningKeys(utxo: SpendableUnspentOutputs, secondPassword: String): Single<List<SigningKey>> {
        if (isHDAccount) {
            if (payloadDataManager.isDoubleEncrypted) {
                payloadDataManager.decryptHDWallet(secondPassword)
            }

            return Single.just(
                payloadDataManager.getHDKeysForSigning(
                    account = internalAccount as Account,
                    unspentOutputBundle = utxo
                )
            )
        } else {
            val password = if (payloadDataManager.isDoubleEncrypted) secondPassword else null
            return Single.just(
                listOf(
                    payloadDataManager.getAddressSigningKey(
                        importedAddress = internalAccount as ImportedAddress,
                        secondPassword = password
                    ) ?: throw IllegalStateException("Private key not found for legacy BTC address")
                )
            )
        }
    }

    fun getChangeAddress(): Single<String> {
        return if (isHDAccount) {
            payloadDataManager.getNextChangeAddress(internalAccount as Account)
                .singleOrError()
        } else {
            Single.just((internalAccount as ImportedAddress).address)
        }
    }

    fun incrementReceiveAddress() {
        if (isHDAccount) {
            val account = internalAccount as Account
            payloadDataManager.incrementChangeAddress(account)
            payloadDataManager.incrementReceiveAddress(account)
        }
    }

    fun getReceiveAddressAtPosition(position: Int): String? {
        require(isHDAccount)
        return payloadDataManager.getReceiveAddressAtPosition(internalAccount as Account, position)
    }

    override fun matches(other: CryptoAccount): Boolean =
        other is BtcCryptoWalletAccount && other.xpubAddress == xpubAddress

    internal fun forceRefresh() {
        refreshTrigger.forceAccountsRefresh()
    }

    override fun doesAddressBelongToWallet(address: String): Boolean =
        payloadDataManager.isOwnHDAddress(address)

    companion object {
        fun createHdAccount(
            jsonAccount: Account,
            payloadDataManager: PayloadDataManager,
            hdAccountIndex: Int,
            sendDataManager: SendDataManager,
            feeDataManager: FeeDataManager,
            exchangeRates: ExchangeRatesDataManager,
            walletPreferences: WalletStatusPrefs,
            refreshTrigger: AccountRefreshTrigger,
            addressResolver: AddressResolver
        ) = BtcCryptoWalletAccount(
            payloadDataManager = payloadDataManager,
            hdAccountIndex = hdAccountIndex,
            sendDataManager = sendDataManager,
            feeDataManager = feeDataManager,
            exchangeRates = exchangeRates,
            internalAccount = jsonAccount,
            isHDAccount = true,
            walletPreferences = walletPreferences,
            refreshTrigger = refreshTrigger,
            addressResolver = addressResolver
        )

        fun createImportedAccount(
            importedAccount: ImportedAddress,
            payloadDataManager: PayloadDataManager,
            sendDataManager: SendDataManager,
            feeDataManager: FeeDataManager,
            exchangeRates: ExchangeRatesDataManager,
            walletPreferences: WalletStatusPrefs,
            refreshTrigger: AccountRefreshTrigger,
            addressResolver: AddressResolver
        ) = BtcCryptoWalletAccount(
            payloadDataManager = payloadDataManager,
            hdAccountIndex = IMPORTED_ACCOUNT_NO_INDEX,
            sendDataManager = sendDataManager,
            feeDataManager = feeDataManager,
            exchangeRates = exchangeRates,
            internalAccount = importedAccount,
            isHDAccount = false,
            walletPreferences = walletPreferences,
            refreshTrigger = refreshTrigger,
            addressResolver = addressResolver
        )

        private const val IMPORTED_ACCOUNT_NO_INDEX = Int.MAX_VALUE

        private const val BTC_PUBKEY_DESCRIPTOR = "p2wpkh"
    }
}
