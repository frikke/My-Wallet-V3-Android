package com.blockchain.coincore.fiat

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AvailableActions
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.FiatActivitySummaryItem
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.TxSourceState
import com.blockchain.coincore.toFiat
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.price.ExchangeRates
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.TransactionState
import com.blockchain.nabu.datamanagers.TransactionType
import com.blockchain.nabu.datamanagers.repositories.interest.IneligibilityReason
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.balance.total
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import java.util.concurrent.atomic.AtomicBoolean

/*internal*/ class FiatCustodialAccount internal constructor(
    override val label: String,
    override val fiatCurrency: String,
    override val isDefault: Boolean = false,
    private val tradingBalanceDataManager: TradingBalanceDataManager,
    private val custodialWalletManager: CustodialWalletManager,
    private val exchangesRates: ExchangeRatesDataManager
) : FiatAccount, TradingAccount {
    private val hasFunds = AtomicBoolean(false)

    override val balance: Observable<AccountBalance>
        get() = Observable.combineLatest(
            tradingBalanceDataManager.getBalanceForFiat(fiatCurrency),
            exchangesRates.fiatToUserFiatRate(fiatCurrency)
        ) { balance, rate ->
            AccountBalance.from(balance, rate)
        }.doOnNext { hasFunds.set(it.total.isPositive) }

    override val accountBalance: Single<Money>
        get() = balance.map { it.total }.firstOrError()

    override val actionableBalance: Single<Money>
        get() = balance.map { it.actionable }.firstOrError()

    override val pendingBalance: Single<Money>
        get() = balance.map { it.total }.firstOrError()

    override var hasTransactions: Boolean = false
        private set

    override val activity: Single<ActivitySummaryList>
        get() = custodialWalletManager.getCustodialFiatTransactions(fiatCurrency, Product.BUY)
            .doOnSuccess {
                setHasTransactions(it.isEmpty().not())
            }.map {
                it.map { fiatTransaction ->
                    FiatActivitySummaryItem(
                        currency = fiatCurrency,
                        exchangeRates = exchangesRates,
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

    override fun canWithdrawFunds(): Single<Boolean> =
        custodialWalletManager.getCustodialFiatTransactions(fiatCurrency, Product.BUY).map {
            it.filter { tx -> tx.type == TransactionType.WITHDRAWAL && tx.state == TransactionState.PENDING }
        }.map {
            it.isEmpty()
        }

    override val actions: Single<AvailableActions> =
        custodialWalletManager.canTransactWithBankMethods(fiatCurrency)
            .zipWith(actionableBalance.map { it.isPositive })
            .map { (canTransactWithBanks, hasActionableBalance) ->
                if (canTransactWithBanks) {
                    setOfNotNull(
                        AssetAction.ViewActivity,
                        AssetAction.FiatDeposit,
                        if (hasActionableBalance) AssetAction.Withdraw else null
                    )
                } else {
                    setOf(AssetAction.ViewActivity)
                }
            }

    override val isFunded: Boolean
        get() = hasFunds.get()

    private fun setHasTransactions(hasTransactions: Boolean) {
        this.hasTransactions = hasTransactions
    }

    override fun fiatBalance(fiatCurrency: String, exchangeRates: ExchangeRates): Single<Money> =
        accountBalance.map { it.toFiat(fiatCurrency, exchangeRates) }

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.error(NotImplementedError("Send to fiat not supported"))

    override val sourceState: Single<TxSourceState>
        get() = Single.just(TxSourceState.NOT_SUPPORTED)

    override val isEnabled: Single<Boolean>
        get() = Single.just(true)

    override val disabledReason: Single<IneligibilityReason>
        get() = Single.just(IneligibilityReason.NONE)
}

class FiatAccountGroup(
    override val label: String,
    override val accounts: SingleAccountList
) : AccountGroup {
    // Produce the sum of all balances of all accounts
    override val balance: Observable<AccountBalance>
        get() = Observable.error(NotImplementedError("No unified balance for All Fiat accounts"))

    override val accountBalance: Single<Money>
        get() = Single.error(NotImplementedError("No unified balance for All Fiat accounts"))

    override val actionableBalance: Single<Money>
        get() = Single.error(NotImplementedError("No unified balance for All Fiat accounts"))

    override val pendingBalance: Single<Money>
        get() = Single.error(NotImplementedError("No unified pending balance for All Fiat accounts"))

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
    override val actions: Single<AvailableActions>
        get() = if (accounts.isEmpty()) {
            Single.just(emptySet())
        } else {
            Single.zip(
                accounts.map { it.actions }
            ) { t: Array<Any> ->
                t.filterIsInstance<AvailableActions>().flatten().toSet()
            }
        }

    // if _any_ of the accounts have transactions
    override val hasTransactions: Boolean
        get() = accounts.map { it.hasTransactions }.any { it }

    // Are any of the accounts funded
    override val isFunded: Boolean = accounts.map { it.isFunded }.any { it }

    override fun fiatBalance(
        fiatCurrency: String,
        exchangeRates: ExchangeRates
    ): Single<Money> =
        if (accounts.isEmpty()) {
            Single.just(FiatValue.zero(fiatCurrency))
        } else {
            Single.zip(
                accounts.map { it.fiatBalance(fiatCurrency, exchangeRates) }
            ) { t: Array<Any> ->
                t.map { it as Money }
                    .total()
            }
        }

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.error(NotImplementedError("No receive addresses for All Fiat accounts"))

    override fun includes(account: BlockchainAccount): Boolean =
        accounts.contains(account)
}
