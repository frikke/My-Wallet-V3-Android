package com.blockchain.coincore.impl

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.AddressResolver
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AvailableActions
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.ExchangeAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.NonCustodialActivitySummaryItem
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.TradeActivitySummaryItem
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.TxSourceState
import com.blockchain.coincore.takeEnabledIf
import com.blockchain.coincore.toUserFiat
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.nabu.Feature
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.nabu.datamanagers.repositories.interest.IneligibilityReason
import com.blockchain.nabu.datamanagers.repositories.swap.TradeTransactionItem
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.balance.asFiatCurrencyOrThrow
import info.blockchain.balance.total
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

internal const val transactionFetchCount = 50
internal const val transactionFetchOffset = 0

abstract class CryptoAccountBase : CryptoAccount {
    protected abstract val exchangeRates: ExchangeRatesDataManager
    protected abstract val baseActions: Set<AssetAction>

    final override var hasTransactions: Boolean = true
        private set

    protected fun setHasTransactions(hasTransactions: Boolean) {
        this.hasTransactions = hasTransactions
    }

    protected abstract val directions: Set<TransferDirection>

    override val sourceState: Single<TxSourceState>
        get() = Single.just(TxSourceState.NOT_SUPPORTED)

    override val isEnabled: Single<Boolean>
        get() = Single.just(true)

    override val disabledReason: Single<IneligibilityReason>
        get() = Single.just(IneligibilityReason.NONE)

    private fun normaliseTxId(txId: String): String =
        txId.replace("-", "")

    protected fun appendTradeActivity(
        custodialWalletManager: CustodialWalletManager,
        asset: AssetInfo,
        activityList: List<ActivitySummaryItem>
    ): Single<ActivitySummaryList> = custodialWalletManager.getCustodialActivityForAsset(asset, directions)
        .map { swapItems ->
            swapItems.map {
                custodialItemToSummary(it)
            }
        }.map { custodialItemsActivity ->
            reconcileSwaps(custodialItemsActivity, activityList)
        }

    private fun custodialItemToSummary(item: TradeTransactionItem): TradeActivitySummaryItem {
        val sendingAccount = this
        val userFiat = item.apiFiatValue.toUserFiat(exchangeRates)
        return with(item) {
            TradeActivitySummaryItem(
                exchangeRates = exchangeRates,
                txId = normaliseTxId(txId),
                timeStampMs = timeStampMs,
                sendingValue = sendingValue,
                sendingAccount = sendingAccount,
                sendingAddress = sendingAddress,
                receivingAddress = receivingAddress,
                state = state,
                direction = direction,
                receivingValue = receivingValue,
                depositNetworkFee = Single.just(Money.zero(item.currencyPair.source)),
                withdrawalNetworkFee = withdrawalNetworkFee,
                currencyPair = item.currencyPair,
                fiatValue = userFiat,
                fiatCurrency = userFiat.currency.asFiatCurrencyOrThrow(),
                price = item.price
            )
        }
    }

    protected abstract fun reconcileSwaps(
        tradeItems: List<TradeActivitySummaryItem>,
        activity: List<ActivitySummaryItem>
    ): List<ActivitySummaryItem>

    companion object {
        val defaultActions = setOf(
            AssetAction.ViewActivity,
            AssetAction.Send,
            AssetAction.InterestDeposit,
            AssetAction.Swap,
            AssetAction.Sell,
            AssetAction.Receive,
            AssetAction.Buy
        )
    }
}

// To handle Send to PIT
/*internal*/ class CryptoExchangeAccount internal constructor(
    override val currency: AssetInfo,
    override val label: String,
    private val address: String,
    override val exchangeRates: ExchangeRatesDataManager
) : CryptoAccountBase(), ExchangeAccount {

    override val baseActions: Set<AssetAction> = setOf()

    override fun requireSecondPassword(): Single<Boolean> =
        Single.just(false)

    override fun matches(other: CryptoAccount): Boolean =
        other is CryptoExchangeAccount && other.currency == currency

    override val balance: Observable<AccountBalance>
        get() = Observable.just(AccountBalance.zero(currency))

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(
            makeExternalAssetAddress(
                asset = currency,
                label = label,
                address = address,
                postTransactions = onTxCompleted
            )
        )

    override val directions: Set<TransferDirection>
        get() = emptySet()

    override val isDefault: Boolean = false
    override val isFunded: Boolean = false

    override val activity: Single<ActivitySummaryList>
        get() = Single.just(emptyList())

    override val actions: Single<AvailableActions> = Single.just(emptySet())

    override val stateAwareActions: Single<Set<StateAwareAction>>
        get() = Single.just(emptySet())

    // No activity on exchange accounts, so just return the activity list
    // unmodified - they should both be empty anyway
    override fun reconcileSwaps(
        tradeItems: List<TradeActivitySummaryItem>,
        activity: List<ActivitySummaryItem>
    ): List<ActivitySummaryItem> = activity
}

abstract class CryptoNonCustodialAccount(
    // TODO: Build an interface on PayloadDataManager/PayloadManager for 'global' crypto calls; second password etc?
    protected val payloadDataManager: PayloadDataManager,
    override val currency: AssetInfo,
    private val custodialWalletManager: CustodialWalletManager,
    private val identity: UserIdentity
) : CryptoAccountBase(), NonCustodialAccount {

    override val isFunded: Boolean = true

    override val balance: Observable<AccountBalance>
        get() = Observable.combineLatest(
            getOnChainBalance(),
            exchangeRates.exchangeRateToUserFiat(currency)
        ) { balance, rate ->
            AccountBalance(
                total = balance,
                withdrawable = balance,
                pending = CryptoValue.zero(currency),
                exchangeRate = rate
            )
        }

    protected abstract val addressResolver: AddressResolver

    protected abstract fun getOnChainBalance(): Observable<Money>

    // The plan here is once we are caching the non custodial balances to remove this isFunded
    override val actions: Single<AvailableActions>
        get() = custodialWalletManager.getSupportedFundsFiats().onErrorReturn { emptyList() }.zipWith(
            identity.isEligibleFor(Feature.Interest(currency))
        ).map { (fiatAccounts, isEligibleForInterest) ->

            val isActiveFunded = !isArchived && isFunded

            val activity = AssetAction.ViewActivity.takeIf { hasTransactions }
            val receive = AssetAction.Receive.takeEnabledIf(baseActions) {
                !isArchived
            }
            val send = AssetAction.Send.takeEnabledIf(baseActions) {
                isActiveFunded
            }
            val swap = AssetAction.Swap.takeEnabledIf(baseActions) {
                isActiveFunded
            }
            val sell = AssetAction.Sell.takeEnabledIf(baseActions) {
                isActiveFunded && fiatAccounts.isNotEmpty()
            }
            val interest = AssetAction.InterestDeposit.takeEnabledIf(baseActions) {
                isActiveFunded && isEligibleForInterest
            }

            setOfNotNull(
                activity, receive, send, swap, sell, interest
            )
        }

    override val stateAwareActions: Single<Set<StateAwareAction>>
        get() = custodialWalletManager.getSupportedFundsFiats().onErrorReturn { emptyList() }.zipWith(
            identity.isEligibleFor(Feature.Interest(currency))
        ).map { (fiatAccounts, isEligibleForInterest) ->

            val isActiveFunded = !isArchived && isFunded

            val activity = StateAwareAction(
                if (hasTransactions) ActionState.Available else ActionState.LockedForOther, AssetAction.ViewActivity
            )

            val receive = StateAwareAction(
                if (baseActions.contains(
                        AssetAction.Receive
                    ) && !isArchived
                ) ActionState.Available else ActionState.LockedForOther,
                AssetAction.Receive
            )

            val send = StateAwareAction(
                if (baseActions.contains(
                        AssetAction.Send
                    ) && isActiveFunded
                ) ActionState.Available else ActionState.LockedForOther,
                AssetAction.Send
            )

            val swap = StateAwareAction(
                if (baseActions.contains(
                        AssetAction.Swap
                    ) && isActiveFunded
                ) ActionState.Available else ActionState.LockedForOther,
                AssetAction.Swap
            )
            val sell = StateAwareAction(
                if (baseActions.contains(
                        AssetAction.Sell
                    ) && isActiveFunded && fiatAccounts.isNotEmpty()
                ) ActionState.Available else ActionState.LockedForOther,
                AssetAction.Sell
            )

            val interest = StateAwareAction(
                if (baseActions.contains(
                        AssetAction.InterestDeposit
                    ) && isActiveFunded && isEligibleForInterest
                ) ActionState.Available else ActionState.LockedForOther,
                AssetAction.InterestDeposit
            )

            setOf(
                activity, receive, send, swap, sell, interest
            )
        }

    override val directions: Set<TransferDirection> = setOf(TransferDirection.FROM_USERKEY, TransferDirection.ON_CHAIN)

    override val sourceState: Single<TxSourceState>
        get() = balance.firstOrError().map {
            if (it.withdrawable.isZero) {
                TxSourceState.NO_FUNDS
            } else {
                TxSourceState.CAN_TRANSACT
            }
        }

    override fun requireSecondPassword(): Single<Boolean> =
        Single.fromCallable { payloadDataManager.isDoubleEncrypted }

    abstract fun createTxEngine(target: TransactionTarget, action: AssetAction): TxEngine

    override val isArchived: Boolean
        get() = false

    override fun reconcileSwaps(
        tradeItems: List<TradeActivitySummaryItem>,
        activity: List<ActivitySummaryItem>
    ): List<ActivitySummaryItem> {
        val activityList = activity.toMutableList()
        tradeItems.forEach { custodialItem ->
            val hit = activityList.find {
                it.txId.contains(custodialItem.txId, true)
            } as? NonCustodialActivitySummaryItem

            if (hit?.transactionType == TransactionSummary.TransactionType.SENT) {
                activityList.remove(hit)
                val updatedSwap = custodialItem.copy(
                    depositNetworkFee = hit.fee.first(Money.zero(hit.asset))
                )
                activityList.add(updatedSwap)
            }
        }
        return activityList.toList()
    }

    // For editing etc
    open fun updateLabel(newLabel: String): Completable =
        Completable.error(UnsupportedOperationException("Cannot update account label for $currency accounts"))

    open fun archive(): Completable =
        Completable.error(UnsupportedOperationException("Cannot archive $currency accounts"))

    open fun unarchive(): Completable =
        Completable.error(UnsupportedOperationException("Cannot unarchive $currency accounts"))

    open fun setAsDefault(): Completable =
        Completable.error(UnsupportedOperationException("$currency doesn't support multiple accounts"))

    open val xpubAddress: String
        get() = throw UnsupportedOperationException("$currency doesn't support xpub")

    override fun matches(other: CryptoAccount): Boolean =
        other is CryptoNonCustodialAccount && other.currency == currency
}

// Currently only one custodial account is supported for each asset,
// so all the methods on this can just delegate directly
// to the (required) CryptoSingleAccountCustodialBase

class CryptoAccountCustodialGroup(
    override val label: String,
    override val accounts: SingleAccountList
) : AccountGroup {

    private val account: CryptoAccountBase

    init {
        require(accounts.size == 1)
        require(accounts[0] is CryptoInterestAccount || accounts[0] is CustodialTradingAccount)
        account = accounts[0] as CryptoAccountBase
    }

    override val isEnabled: Single<Boolean>
        get() = account.isEnabled

    override val receiveAddress: Single<ReceiveAddress>
        get() = account.receiveAddress

    override val balance: Observable<AccountBalance>
        get() = account.balance

    override val activity: Single<ActivitySummaryList>
        get() = account.activity

    override val actions: Single<AvailableActions>
        get() = account.actions

    override val stateAwareActions: Single<Set<StateAwareAction>>
        get() = account.stateAwareActions

    override val isFunded: Boolean
        get() = account.isFunded

    override val hasTransactions: Boolean
        get() = account.hasTransactions

    override fun includes(account: BlockchainAccount): Boolean =
        accounts.contains(account)
}

class CryptoAccountNonCustodialGroup(
    val asset: AssetInfo,
    override val label: String,
    override val accounts: SingleAccountList
) : AccountGroup {

    // Produce the sum of all balances of all accounts
    override val balance: Observable<AccountBalance>
        get() = if (accounts.isEmpty()) {
            Observable.just(AccountBalance.zero(asset))
        } else {
            Observable.zip(
                accounts.map { it.balance }
            ) { t: Array<Any> ->
                val balances = t.map { it as AccountBalance }
                AccountBalance(
                    total = balances.map { it.total }.total(),
                    withdrawable = balances.map { it.withdrawable }.total(),
                    pending = balances.map { it.pending }.total(),
                    exchangeRate = balances.first().exchangeRate
                )
            }
        }

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
            Single.zip(accounts.map { it.actions }) { t: Array<Any> ->
                t.filterIsInstance<AvailableActions>().flatten().toSet()
            }
        }

    override val stateAwareActions: Single<Set<StateAwareAction>>
        get() = if (accounts.isEmpty()) {
            Single.just(emptySet())
        } else {
            Single.zip(accounts.map { it.stateAwareActions }) { t: Array<Any> ->
                t.filterIsInstance<StateAwareAction>().toSet()
            }
        }

    // if _any_ of the accounts have transactions
    override val hasTransactions: Boolean
        get() = accounts.map { it.hasTransactions }.any { it }

    // Are any of the accounts funded
    override val isFunded: Boolean = accounts.map { it.isFunded }.any { it }

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.error(IllegalStateException("Accessing receive address on a group is not allowed"))

    override fun includes(account: BlockchainAccount): Boolean =
        accounts.contains(account)
}
