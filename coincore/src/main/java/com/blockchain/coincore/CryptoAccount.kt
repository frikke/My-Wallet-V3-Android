package com.blockchain.coincore

import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.core.custodial.TradingAccountBalance
import com.blockchain.core.interest.InterestAccountBalance
import com.blockchain.core.price.ExchangeRate
import com.blockchain.nabu.datamanagers.repositories.interest.IneligibilityReason
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

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

    val balance: Observable<AccountBalance>

    val activity: Single<ActivitySummaryList>

    val isFunded: Boolean

    val hasTransactions: Boolean

    val disabledReason: Single<IneligibilityReason>

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

    override val disabledReason: Single<IneligibilityReason>
        get() = Single.just(IneligibilityReason.NONE)

    fun includes(account: BlockchainAccount): Boolean
}

internal fun BlockchainAccount.isCustodial(): Boolean =
    this is CustodialTradingAccount
