package com.blockchain.coincore

import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.core.custodial.domain.model.TradingAccountBalance
import com.blockchain.core.interest.domain.model.InterestAccountBalance
import com.blockchain.core.staking.domain.model.StakingAccountBalance
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.rx3.asFlow

data class AccountBalance internal constructor(
    val total: Money,
    val withdrawable: Money,
    val pending: Money,
    val exchangeRate: ExchangeRate,
) {
    val totalFiat: Money by lazy {
        exchangeRate.convert(total)
    }

    companion object {
        internal fun from(balance: TradingAccountBalance, rate: ExchangeRate): AccountBalance {
            return AccountBalance(
                total = balance.total,
                withdrawable = balance.withdrawable,
                pending = balance.pending,
                exchangeRate = rate
            )
        }

        internal fun totalOf(first: AccountBalance, second: AccountBalance): AccountBalance {
            require(first.total.currency == second.total.currency) {
                "total of different Account balances is not supported"
            }
            return AccountBalance(
                total = first.total + second.total,
                withdrawable = first.withdrawable + second.withdrawable,
                pending = first.pending + second.pending,
                exchangeRate = first.exchangeRate
            )
        }

        internal fun from(balance: InterestAccountBalance, rate: ExchangeRate): AccountBalance {
            return AccountBalance(
                total = balance.totalBalance,
                withdrawable = balance.actionableBalance,
                pending = balance.pendingDeposit,
                exchangeRate = rate
            )
        }

        internal fun from(balance: StakingAccountBalance, rate: ExchangeRate): AccountBalance =
            AccountBalance(
                total = balance.totalBalance,
                withdrawable = balance.availableBalance,
                pending = balance.pendingDeposit,
                exchangeRate = rate
            )

        fun zero(assetInfo: Currency) =
            AccountBalance(
                total = Money.zero(assetInfo),
                withdrawable = Money.zero(assetInfo),
                pending = Money.zero(assetInfo),
                exchangeRate = ExchangeRate.zeroRateExchangeRate(assetInfo)
            )
    }
}

interface BlockchainAccount {

    val label: String

    val balanceRx: Observable<AccountBalance>

    val balance: Flow<AccountBalance>
        get() = balanceRx.asFlow()

    val activity: Single<ActivitySummaryList>

    val isFunded: Boolean

    val hasTransactions: Boolean

    val receiveAddress: Single<ReceiveAddress>

    fun requireSecondPassword(): Single<Boolean> = Single.just(false)

    val stateAwareActions: Single<Set<StateAwareAction>>
}

interface SingleAccount : BlockchainAccount, TransactionTarget {
    val isDefault: Boolean

    val currency: Currency

    // Is this account currently able to operate as a transaction source
    val sourceState: Single<TxSourceState>

    val isMemoSupported: Boolean
        get() = false

    fun doesAddressBelongToWallet(address: String): Boolean = false
}

typealias AccountsSorter = (List<SingleAccount>) -> Single<List<SingleAccount>>

enum class TxSourceState {
    CAN_TRANSACT,
    NO_FUNDS,
    FUNDS_LOCKED,
    NOT_ENOUGH_GAS,
    TRANSACTION_IN_FLIGHT,
    NOT_SUPPORTED
}

interface InterestAccount
interface TradingAccount
interface NonCustodialAccount
interface BankAccount
interface ExchangeAccount
interface StakingAccount

typealias SingleAccountList = List<SingleAccount>

interface CryptoAccount : SingleAccount {
    val isArchived: Boolean
        get() = false

    override val currency: AssetInfo

    fun matches(other: CryptoAccount): Boolean

    val hasStaticAddress: Boolean
        get() = true
}

interface FiatAccount : SingleAccount {
    fun canWithdrawFunds(): Single<Boolean>
    override val currency: FiatCurrency
}

interface AccountGroup : BlockchainAccount {
    val accounts: SingleAccountList

    override val activity: Single<ActivitySummaryList>
        get() = allActivities()

    fun includes(account: BlockchainAccount): Boolean =
        accounts.contains(account)

    override val stateAwareActions: Single<Set<StateAwareAction>>
        get() = Single.just(
            setOf(
                StateAwareAction(ActionState.Available, AssetAction.ViewActivity)
            )
        )

    override val receiveAddress: Single<ReceiveAddress>
        get() = throw IllegalStateException("ReceiveAddress is not supported")

    /**
     * TODO remove those from the interface of account
     */
    override val isFunded: Boolean
        get() = true

    override val hasTransactions: Boolean
        get() = true

    private fun allActivities(): Single<ActivitySummaryList> =
        Single.just(accounts).flattenAsObservable { it }
            .flatMapSingle { account ->
                account.activity
                    .onErrorResumeNext { Single.just(emptyList()) }
            }
            .reduce { a, l -> a + l }
            .defaultIfEmpty(emptyList())
            .map { it.distinct() }
            .map { it.sorted() }
}

interface SameCurrencyAccountGroup : AccountGroup {
    val currency: Currency

    override val balanceRx: Observable<AccountBalance>
        get() = Single.just(accounts).flattenAsObservable { it }.flatMapSingle {
            it.balanceRx.firstOrError()
        }.reduce { a, v ->
            AccountBalance.totalOf(a, v)
        }.toObservable()
}

interface MultipleCurrenciesAccountGroup : AccountGroup {
    /**
     * Balance is calculated in the selected fiat currency
     */
    override val balanceRx: Observable<AccountBalance>
        get() =
            if (accounts.isEmpty())
                Observable.just(AccountBalance.zero(baseCurrency))
            else
                Single.just(accounts).flattenAsObservable { it }.flatMapSingle { account ->
                    account.balanceRx.firstOrError()
                }.reduce { a, v ->
                    AccountBalance(
                        total = a.exchangeRate.convert(a.total) + v.exchangeRate.convert(v.total),
                        withdrawable = a.exchangeRate.convert(a.withdrawable) + v.exchangeRate.convert(v.withdrawable),
                        pending = a.exchangeRate.convert(a.pending) + v.exchangeRate.convert(v.pending),
                        exchangeRate = ExchangeRate.identityExchangeRate(a.exchangeRate.to)
                    )
                }.toObservable()

    val baseCurrency: Currency
}

internal fun BlockchainAccount.isTrading(): Boolean =
    this is CustodialTradingAccount
