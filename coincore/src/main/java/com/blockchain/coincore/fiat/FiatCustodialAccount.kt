package com.blockchain.coincore.fiat

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.ActionState
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
import com.blockchain.data.dataOrNull
import com.blockchain.data.mapData
import com.blockchain.data.toObservable
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.TransactionState
import com.blockchain.nabu.datamanagers.TransactionType
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.rx3.asObservable

class FiatCustodialAccount internal constructor(
    override val label: String,
    override val currency: FiatCurrency,
    override val isDefault: Boolean = false,
    private val tradingService: TradingService,
    private val bankService: BankService,
    private val simpleBuyService: SimpleBuyService,
    private val exchangeRates: ExchangeRatesDataManager
) : FiatAccount, TradingAccount {
    private val hasFunds = AtomicBoolean(false)

    override fun balanceRx(freshnessStrategy: FreshnessStrategy): Observable<AccountBalance> =
        Observable.combineLatest(
            tradingService.getBalanceFor(
                currency,
                freshnessStrategy
            ),
            exchangeRates.exchangeRateToUserFiatFlow(currency).asObservable()
        ) { balance, rate ->
            AccountBalance(
                total = balance.total,
                withdrawable = balance.withdrawable,
                pending = balance.pending,
                exchangeRate = rate.dataOrNull()
            )
        }.doOnNext { hasFunds.set(it.total.isPositive) }

    override fun activity(freshnessStrategy: FreshnessStrategy): Observable<ActivitySummaryList> {
        return exchangeRates.exchangeRateToUserFiat(currency).flatMap { exchangeRate ->
            simpleBuyService.getFiatTransactions(
                freshnessStrategy = freshnessStrategy,
                fiatCurrency = currency,
                product = Product.BUY
            ).mapData {
                it.map { fiatTransaction ->
                    FiatActivitySummaryItem(
                        currency = currency,
                        exchangeRates = exchangeRates,
                        txId = fiatTransaction.id,
                        fiat = exchangeRate.convert(fiatTransaction.amount),
                        timeStampMs = fiatTransaction.date.time,
                        value = fiatTransaction.amount,
                        account = this,
                        state = fiatTransaction.state,
                        type = fiatTransaction.type,
                        paymentMethodId = fiatTransaction.paymentId
                    )
                }
            }.toObservable()
        }
    }

    override fun canWithdrawFunds(): Flow<DataResource<Boolean>> =
        simpleBuyService.getFiatTransactions(
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
            .zipWith(balanceRx().firstOrError().map { it.withdrawable.isPositive })
            .map { (canTransactWithBanks, hasActionableBalance) ->
                if (canTransactWithBanks) {
                    setOfNotNull(
                        StateAwareAction(ActionState.Available, AssetAction.ViewActivity),
                        StateAwareAction(ActionState.Available, AssetAction.FiatDeposit),
                        if (hasActionableBalance) {
                            StateAwareAction(
                                ActionState.Available,
                                AssetAction.FiatWithdraw
                            )
                        } else {
                            null
                        }
                    )
                } else {
                    setOf(StateAwareAction(ActionState.Available, AssetAction.ViewActivity))
                }
            }

    override fun stateOfAction(assetAction: AssetAction): Single<ActionState> {
        return stateAwareActions.map { set ->
            set.firstOrNull { it.action == assetAction }?.state ?: ActionState.Unavailable
        }
    }

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.error(NotImplementedError("Send to fiat not supported"))

    override val sourceState: Single<TxSourceState>
        get() = Single.just(TxSourceState.NOT_SUPPORTED)

    override fun matches(other: FiatAccount): Boolean =
        other is FiatCustodialAccount && currency == other.currency
}

class FiatAccountGroup(
    override val label: String,
    override val accounts: SingleAccountList
) : SameCurrencyAccountGroup {
    override val currency: Currency
        get() = accounts[0].currency

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

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.error(NotImplementedError("No receive addresses for All Fiat accounts"))

    override fun includes(account: BlockchainAccount): Boolean =
        accounts.contains(account)
}
