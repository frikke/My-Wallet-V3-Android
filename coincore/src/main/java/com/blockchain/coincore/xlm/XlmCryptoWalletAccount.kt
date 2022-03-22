package com.blockchain.coincore.xlm

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.AddressResolver
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.WalletStatus
import com.blockchain.sunriver.BalanceAndMin
import com.blockchain.sunriver.XlmAccountReference
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.XlmFeesFetcher
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.balance.Money.Companion.max
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.atomic.AtomicBoolean
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.androidcore.utils.extensions.mapList

internal class XlmCryptoWalletAccount(
    payloadManager: PayloadDataManager,
    private var xlmAccountReference: XlmAccountReference,
    private val xlmManager: XlmDataManager,
    override val exchangeRates: ExchangeRatesDataManager,
    private val xlmFeesFetcher: XlmFeesFetcher,
    private val walletOptionsDataManager: WalletOptionsDataManager,
    private val walletPreferences: WalletStatus,
    private val custodialWalletManager: CustodialWalletManager,
    identity: UserIdentity,
    override val addressResolver: AddressResolver
) : CryptoNonCustodialAccount(
    payloadManager, CryptoCurrency.XLM, custodialWalletManager, identity
) {

    override val baseActions: Set<AssetAction> = defaultActions
    override val isDefault: Boolean = true // Only one account ever, so always default

    override val label: String
        get() = xlmAccountReference.label

    internal val address: String
        get() = xlmAccountReference.accountId

    private val hasFunds = AtomicBoolean(false)

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val balance: Observable<AccountBalance>
        get() = Observable.combineLatest(
            getMinBalance(),
            exchangeRates.exchangeRateToUserFiat(currency)
        ) { balanceAndMin, rate ->
            AccountBalance(
                total = balanceAndMin.balance,
                withdrawable = balanceAndMin.actionable,
                pending = Money.zero(currency),
                exchangeRate = rate
            )
        }.doOnNext { hasFunds.set(it.total.isPositive) }

    override fun getOnChainBalance(): Observable<Money> =
        getMinBalance()
            .doOnNext { hasFunds.set(it.balance.isPositive) }
            .map { it.balance as Money }

    private fun getMinBalance(): Observable<BalanceAndMin> =
        xlmManager.getBalanceAndMin()
            .toObservable()

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(
            XlmAddress(_address = address, _label = label)
        )

    override val activity: Single<ActivitySummaryList>
        get() = xlmManager.getTransactionList()
            .onErrorResumeNext { Single.just(emptyList()) }
            .mapList {
                XlmActivitySummaryItem(
                    it,
                    exchangeRates,
                    account = this,
                    payloadDataManager
                )
            }.flatMap {
                appendTradeActivity(custodialWalletManager, currency, it)
            }.doOnSuccess { setHasTransactions(it.isNotEmpty()) }

    override fun updateLabel(newLabel: String): Completable {
        require(newLabel.isNotEmpty())
        if (newLabel == label) return Completable.complete()
        val revertLabel = label
        xlmAccountReference = xlmAccountReference.copy(label = newLabel)
        return xlmManager.updateAccountLabel(newLabel)
            .doOnError { xlmAccountReference = xlmAccountReference.copy(label = revertLabel) }
    }

    override fun createTxEngine(target: TransactionTarget, action: AssetAction): TxEngine =
        XlmOnChainTxEngine(
            xlmDataManager = xlmManager,
            xlmFeesFetcher = xlmFeesFetcher,
            walletOptionsDataManager = walletOptionsDataManager,
            requireSecondPassword = payloadDataManager.isDoubleEncrypted,
            walletPreferences = walletPreferences,
            resolvedAddress = addressResolver.getReceiveAddress(currency, target, action)
        )
}

private val BalanceAndMin.actionable: Money
    get() = max(balance - minimumBalance, CryptoValue.zero(CryptoCurrency.XLM))
