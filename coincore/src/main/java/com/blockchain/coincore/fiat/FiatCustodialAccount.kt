package com.blockchain.coincore.fiat

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.FiatActivitySummaryItem
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SameCurrencyAccountGroup
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.TxSourceState
import com.blockchain.core.buy.domain.SimpleBuyService
import com.blockchain.core.custodial.domain.TradingService
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.TransactionState
import com.blockchain.nabu.datamanagers.TransactionType
import com.blockchain.store.asSingle
import com.blockchain.store.mapData
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.util.concurrent.atomic.AtomicBoolean

/*internal*/ class FiatCustodialAccount internal constructor(
    override val label: String,
    override val currency: FiatCurrency,
    override val isDefault: Boolean = false,
    private val tradingService: TradingService,
    private val custodialWalletManager: CustodialWalletManager,
    private val bankService: BankService,
    private val simpleBuyService: SimpleBuyService,
    private val exchangeRates: ExchangeRatesDataManager
) : FiatAccount, TradingAccount {
    private val hasFunds = AtomicBoolean(false)

    override val balanceRx: Observable<AccountBalance>
        get() = Observable.combineLatest(
            tradingService.getBalanceFor(currency),
            exchangeRates.exchangeRateToUserFiat(currency)
        ) { balance, rate ->
            AccountBalance(
                total = balance.total,
                withdrawable = balance.withdrawable,
                pending = balance.pending,
                dashboardDisplay = balance.dashboardDisplay,
                exchangeRate = rate,
            )
        }.doOnNext { hasFunds.set(it.total.isPositive) }

    override var hasTransactions: Boolean = false
        private set

    override val activity: Single<ActivitySummaryList>
        get() = simpleBuyService.getFiatTransactions(fiatCurrency = currency, product = Product.BUY)
            .asSingle()
            .doOnSuccess {
                setHasTransactions(it.isEmpty().not())
            }.map {
                it.map { fiatTransaction ->
                    FiatActivitySummaryItem(
                        currency = currency,
                        exchangeRates = exchangeRates,
                        txId = fiatTransaction.id,
                        timeStampMs = fiatTransaction.date.time,
                        value = fiatTransaction.amount,
                        account = this,
                        state = fiatTransaction.state,
                        type = fiatTransaction.type,
                        paymentMethodId = fiatTransaction.paymentId
                    )
                }
            }

    override fun canWithdrawFundsLegacy(): Single<Boolean> =
        simpleBuyService.getFiatTransactions(fiatCurrency = currency, product = Product.BUY)
            .asSingle()
            .map {
                it.filter { tx -> tx.type == TransactionType.WITHDRAWAL && tx.state == TransactionState.PENDING }
            }.map {
                it.isEmpty()
            }

    override fun canWithdrawFunds(): Flow<DataResource<Boolean>> =
        simpleBuyService.getFiatTransactions(
            FreshnessStrategy.Cached(forceRefresh = false),
            fiatCurrency = currency,
            product = Product.BUY
        )
            .mapData {
                it.filter { tx -> tx.type == TransactionType.WITHDRAWAL && tx.state == TransactionState.PENDING }
            }
            .mapData { it.isEmpty() }
            .catch { emit(DataResource.Error(Exception("failed getFiatTransactions"))) }

    override val stateAwareActions: Single<Set<StateAwareAction>>
        get() = bankService.canTransactWithBankMethods(currency)
            .zipWith(balanceRx.firstOrError().map { it.withdrawable.isPositive })
            .map { (canTransactWithBanks, hasActionableBalance) ->
                if (canTransactWithBanks) {
                    setOfNotNull(
                        StateAwareAction(ActionState.Available, AssetAction.ViewActivity),
                        StateAwareAction(ActionState.Available, AssetAction.FiatDeposit),
                        if (hasActionableBalance) StateAwareAction(
                            ActionState.Available, AssetAction.FiatWithdraw
                        ) else null
                    )
                } else {
                    setOf(StateAwareAction(ActionState.Available, AssetAction.ViewActivity))
                }
            }

    override val isFunded: Boolean
        get() = hasFunds.get()

    private fun setHasTransactions(hasTransactions: Boolean) {
        this.hasTransactions = hasTransactions
    }

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.error(NotImplementedError("Send to fiat not supported"))

    override val sourceState: Single<TxSourceState>
        get() = Single.just(TxSourceState.NOT_SUPPORTED)
}

class FiatAccountGroup(
    override val label: String,
    override val accounts: SingleAccountList
) : SameCurrencyAccountGroup {
    override val currency: Currency
        get() = accounts[0].currency

    // All the activities for all the accounts
    override val activity: Single<ActivitySummaryList>
        get() = if (accounts.isEmpty()) {
            Single.just(emptyList())
        } else {
            Single.zip(
                accounts.map { it.activity }
            ) { t: Array<Any> ->
                t.filterIsInstance<List<ActivitySummaryItem>>().flatten()
            }
        }

    // The intersection of the actions for each account
    override val stateAwareActions: Single<Set<StateAwareAction>>
        get() = if (accounts.isEmpty()) {
            Single.just(emptySet())
        } else {
            Single.zip(
                accounts.map { it.stateAwareActions }
            ) { t: Array<Any> ->
                t.filterIsInstance<StateAwareAction>().toSet()
            }
        }

    // if _any_ of the accounts have transactions
    override val hasTransactions: Boolean
        get() = accounts.map { it.hasTransactions }.any { it }

    // Are any of the accounts funded
    override val isFunded: Boolean = accounts.map { it.isFunded }.any { it }

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.error(NotImplementedError("No receive addresses for All Fiat accounts"))

    override fun includes(account: BlockchainAccount): Boolean =
        accounts.contains(account)
}
