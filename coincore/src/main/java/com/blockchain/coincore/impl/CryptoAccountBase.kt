package com.blockchain.coincore.impl

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.AddressResolver
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.ExchangeAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.NonCustodialActivitySummaryItem
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SameCurrencyAccountGroup
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.TradeActivitySummaryItem
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.TxSourceState
import com.blockchain.coincore.toActionState
import com.blockchain.coincore.toUserFiat
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.nabu.datamanagers.repositories.swap.TradeTransactionItem
import com.blockchain.store.asObservable
import com.blockchain.unifiedcryptowallet.domain.balances.NetworkBalance
import com.blockchain.unifiedcryptowallet.domain.balances.UnifiedBalancesService
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet
import com.blockchain.utils.zipSingles
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import info.blockchain.balance.asFiatCurrencyOrThrow
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.rxSingle
import org.koin.core.component.KoinComponent

internal const val transactionFetchCount = 50
internal const val transactionFetchOffset = 0

abstract class CryptoAccountBase : CryptoAccount {
    protected abstract val exchangeRates: ExchangeRatesDataManager
    protected abstract val baseActions: Single<Set<AssetAction>>

    final override var hasTransactions: Boolean = true
        private set

    protected fun setHasTransactions(hasTransactions: Boolean) {
        this.hasTransactions = hasTransactions
    }

    protected abstract val directions: Set<TransferDirection>

    override val sourceState: Single<TxSourceState>
        get() = Single.just(TxSourceState.NOT_SUPPORTED)

    private fun normaliseTxId(txId: String): String =
        txId.replace("-", "")

    protected fun appendTradeActivity(
        custodialWalletManager: CustodialWalletManager,
        asset: AssetInfo,
        activityList: List<ActivitySummaryItem>,
    ): Single<ActivitySummaryList> = custodialWalletManager.getCustodialActivityForAsset(asset, directions)
        .map { swapItems ->
            swapItems.map {
                custodialItemToSummary(it)
            }
        }.map { custodialItemsActivity ->
            reconcileSwaps(custodialItemsActivity, activityList)
        }.onErrorReturn { activityList }

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
        activity: List<ActivitySummaryItem>,
    ): List<ActivitySummaryItem>

    companion object {
        internal val defaultNonCustodialActions = setOf(
            AssetAction.ViewActivity,
            AssetAction.Send,
            AssetAction.Sell,
            AssetAction.InterestDeposit,
            AssetAction.StakingDeposit,
            AssetAction.Swap,
            AssetAction.Receive
        )

        internal val defaultCustodialActions = defaultNonCustodialActions + setOf(
            AssetAction.Buy, AssetAction.InterestWithdraw
        )
        internal val defaultActions = (defaultNonCustodialActions + defaultCustodialActions).toSet()
    }
}

// To handle Send to PIT
/*internal*/ class CryptoExchangeAccount internal constructor(
    override val currency: AssetInfo,
    override val label: String,
    private val address: String,
    override val exchangeRates: ExchangeRatesDataManager,
) : CryptoAccountBase(), ExchangeAccount {

    override val baseActions: Single<Set<AssetAction>> = Single.just(emptySet())

    override fun requireSecondPassword(): Single<Boolean> =
        Single.just(false)

    override fun matches(other: CryptoAccount): Boolean =
        other is CryptoExchangeAccount && other.currency == currency

    override fun balanceRx(freshnessStrategy: FreshnessStrategy): Observable<AccountBalance> =
        Observable.just(AccountBalance.zero(currency))

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

    override val stateAwareActions: Single<Set<StateAwareAction>>
        get() = Single.just(emptySet())

    override fun stateOfAction(assetAction: AssetAction): Single<ActionState> {
        return Single.just(ActionState.Unavailable)
    }

    // No activity on exchange accounts, so just return the activity list
    // unmodified - they should both be empty anyway
    override fun reconcileSwaps(
        tradeItems: List<TradeActivitySummaryItem>,
        activity: List<ActivitySummaryItem>,
    ): List<ActivitySummaryItem> = activity
}

abstract class CryptoNonCustodialAccount(
    override val currency: AssetInfo
) : CryptoAccountBase(),
    NetworkWallet,
    NonCustodialAccount,
    KoinComponent {

    override val isFunded: Boolean
        get() = hasFunds.get()

    private val hasFunds = AtomicBoolean(false)

    // TODO: Build an interface on PayloadDataManager/PayloadManager for 'global' crypto calls; second password etc?
    private val payloadDataManager: PayloadDataManager by scopedInject()
    private val unifiedBalancesService: UnifiedBalancesService by scopedInject()
    private val walletModeService: WalletModeService by scopedInject()
    private val identity: UserIdentity by scopedInject()
    private val custodialWalletManager: CustodialWalletManager by scopedInject()

    final override val baseActions: Single<Set<AssetAction>>
        get() = walletModeService.walletModeSingle.map {
            when (it) {
                WalletMode.CUSTODIAL_ONLY -> defaultCustodialActions
                WalletMode.NON_CUSTODIAL_ONLY -> defaultNonCustodialActions
                WalletMode.UNIVERSAL -> defaultActions
            }
        }

    /**
     * We only add this method to make it portable with NetworkWallet.
     */
    override val networkBalance: Flow<DataResource<NetworkBalance>>
        get() = balance().map {
            DataResource.Data(
                NetworkBalance(
                    currency = currency,
                    balance = it.total,
                    unconfirmedBalance = it.pending,
                    exchangeRate = it.exchangeRate
                )
            )
        }

    override fun balanceRx(freshnessStrategy: FreshnessStrategy): Observable<AccountBalance> {
        return unifiedBalancesService.balanceForWallet(
            this@CryptoNonCustodialAccount,
            freshnessStrategy
        ).asObservable().map {
            AccountBalance(
                total = it.balance,
                pending = it.unconfirmedBalance,
                exchangeRate = it.exchangeRate,
                withdrawable = it.balance,
                dashboardDisplay = it.balance,
            )
        }.doOnNext { hasFunds.set(it.total.isPositive) }
    }

    /**
     * Remove after unified balances are fully integrated and stable
     */
    protected abstract fun getOnChainBalance(): Observable<out Money>

    protected abstract val addressResolver: AddressResolver

    override val stateAwareActions: Single<Set<StateAwareAction>>
        get() = baseActions.flatMap { actions ->
            actions.map { it.eligibility() }.zipSingles()
                .map { it.toSet() }
        }

    override fun stateOfAction(assetAction: AssetAction): Single<ActionState> {
        return assetAction.eligibility().map { it.state }
    }

    override val directions: Set<TransferDirection> = setOf(TransferDirection.FROM_USERKEY, TransferDirection.ON_CHAIN)

    override val sourceState: Single<TxSourceState>
        get() = balanceRx().firstOrError().map {
            if (it.withdrawable.isZero) {
                TxSourceState.NO_FUNDS
            } else {
                TxSourceState.CAN_TRANSACT
            }
        }

    /*
    * TODO(antonis-bc) remove this from account
    * */
    override fun requireSecondPassword(): Single<Boolean> =
        Single.fromCallable { payloadDataManager.isDoubleEncrypted }

    abstract fun createTxEngine(target: TransactionTarget, action: AssetAction): TxEngine

    override val isArchived: Boolean
        get() = false

    override fun reconcileSwaps(
        tradeItems: List<TradeActivitySummaryItem>,
        activity: List<ActivitySummaryItem>,
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

    private fun AssetAction.eligibility(): Single<StateAwareAction> {
        val balance = balanceRx().firstOrError().onErrorReturn {
            AccountBalance.zero(currency)
        }

        val isActive = !isArchived

        return when (this) {
            AssetAction.ViewActivity -> Single.just(StateAwareAction(ActionState.Available, this))
            AssetAction.Receive -> Single.just(
                StateAwareAction(
                    if (!isArchived) ActionState.Available else ActionState.Unavailable,
                    this
                )
            )
            AssetAction.Send ->
                balance
                    .flatMap { sendActionEligibility(isActive && it.total.isPositive) }
            AssetAction.Swap ->
                balance
                    .flatMap {
                        swapActionEligibility(isActive && it.total.isPositive)
                    }
            AssetAction.Sell ->
                balance
                    .flatMap {
                        sellActionEligibility(isActive && it.total.isPositive)
                    }
            AssetAction.InterestDeposit ->
                balance
                    .flatMap {
                        interestDepositActionEligibility(isActive && it.total.isPositive)
                    }
            AssetAction.StakingDeposit ->
                balance
                    .flatMap {
                        stakingDepositEligibility(isActive && it.total.isPositive)
                    }
            AssetAction.ViewStatement,
            AssetAction.Buy,
            AssetAction.FiatWithdraw,
            AssetAction.InterestWithdraw,
            AssetAction.FiatDeposit,
            AssetAction.Sign -> Single.just(StateAwareAction(ActionState.Unavailable, this))
        }
    }

    private fun sellActionEligibility(activeAndFunded: Boolean): Single<StateAwareAction> {
        val sellEligibility = identity.userAccessForFeature(Feature.Sell)
        val fiatAccounts = rxSingle { custodialWalletManager.getSupportedFundsFiats().first() }
            .onErrorReturn { emptyList() }
        return sellEligibility.zipWith(fiatAccounts) { sellEligible, fiatAccountsSupported ->
            StateAwareAction(
                when {
                    sellEligible is FeatureAccess.Blocked -> sellEligible.toActionState()
                    fiatAccountsSupported.isEmpty() -> ActionState.LockedForTier
                    !activeAndFunded -> ActionState.LockedForBalance
                    else -> ActionState.Available
                },
                AssetAction.Sell
            )
        }
    }

    private fun interestDepositActionEligibility(activeAndFunded: Boolean): Single<StateAwareAction> {
        val depositCryptoEligibility = identity.userAccessForFeature(Feature.DepositCrypto)
        val currencyInterestEligibility = identity.isEligibleFor(Feature.Interest(currency))
        val interestDepositEligibility = identity.userAccessForFeature(Feature.DepositInterest)
        return Single.zip(
            depositCryptoEligibility,
            currencyInterestEligibility,
            interestDepositEligibility
        ) { depositCryptoEligible, interestEligible, interestDepositEligible ->
            StateAwareAction(
                when {
                    depositCryptoEligible is FeatureAccess.Blocked -> depositCryptoEligible.toActionState()
                    interestDepositEligible is FeatureAccess.Blocked -> interestDepositEligible.toActionState()
                    !interestEligible -> ActionState.LockedForTier
                    !activeAndFunded -> ActionState.LockedForBalance
                    else -> ActionState.Available
                },
                AssetAction.InterestDeposit
            )
        }
    }

    private fun stakingDepositEligibility(activeAndFunded: Boolean): Single<StateAwareAction> {
        val depositCryptoEligibility = identity.userAccessForFeature(Feature.DepositCrypto)
        val stakingDepositEligibility = identity.userAccessForFeature(Feature.DepositStaking)

        return Single.zip(
            depositCryptoEligibility,
            stakingDepositEligibility
        ) { depositCryptoEligible, stakingDepositEligible ->
            StateAwareAction(
                when {
                    depositCryptoEligible is FeatureAccess.Blocked -> depositCryptoEligible.toActionState()
                    stakingDepositEligible is FeatureAccess.Blocked -> stakingDepositEligible.toActionState()
                    !activeAndFunded -> ActionState.LockedForBalance
                    else -> ActionState.Available
                },
                AssetAction.StakingDeposit
            )
        }
    }

    private fun swapActionEligibility(activeAndFunded: Boolean): Single<StateAwareAction> {
        val swapEligibility = identity.userAccessForFeature(Feature.Swap)
        val assetAvailableForSwap = custodialWalletManager.isAssetSupportedForSwapLegacy(currency)
        return swapEligibility.zipWith(assetAvailableForSwap) { swapEligible, assetEligibleForSwap ->
            StateAwareAction(
                when {
                    !assetEligibleForSwap -> ActionState.Unavailable
                    swapEligible is FeatureAccess.Blocked -> swapEligible.toActionState()
                    !activeAndFunded -> ActionState.LockedForBalance
                    else -> ActionState.Available
                },
                AssetAction.Swap
            )
        }
    }

    private fun sendActionEligibility(isActiveAndFunded: Boolean): Single<StateAwareAction> =
        Single.just(
            StateAwareAction(
                when {
                    isActiveAndFunded -> ActionState.Available
                    !isActiveAndFunded -> ActionState.LockedForBalance
                    else -> ActionState.Unavailable
                },
                AssetAction.Send
            )
        )
}

/**
 * Shared group between all custodial accounts
 * Currently only one custodial account per type (Interest, Custodial, Staking) is supported for each asset,
 * so all the methods on this can just delegate directly
 * to the (required) CryptoSingleAccountCustodialBase
 */
class CryptoAccountCustodialSingleGroup(
    override val label: String,
    override val accounts: SingleAccountList,
) : SameCurrencyAccountGroup {

    init {
        require(accounts.size == 1)
        require(
            accounts[0] is CustodialInterestAccount ||
                accounts[0] is CustodialTradingAccount ||
                accounts[0] is CustodialStakingAccount
        )
    }

    override val currency: Currency
        get() = accounts[0].currency
}

/**
 * Group for Trading, Staking and Interest accounts
 */
class CryptoAccountCustodialGroup(
    override val label: String,
    override val accounts: SingleAccountList,
) : SameCurrencyAccountGroup {

    init {
        require(accounts.size in 1..3)
        require(
            accounts.map {
                it is CustodialTradingAccount ||
                    it is CustodialInterestAccount ||
                    it is CustodialStakingAccount
            }.all { it }
        )
        if (accounts.size == 3) {
            require(
                accounts[0].currency == accounts[1].currency &&
                    accounts[0].currency == accounts[2].currency &&
                    accounts[1].currency == accounts[2].currency
            )
        }
    }

    override val currency: Currency
        get() = accounts.first().currency
}

class CryptoAccountNonCustodialGroup(
    val asset: AssetInfo,
    override val label: String,
    override val accounts: SingleAccountList,
) : SameCurrencyAccountGroup {

    override val currency: Currency
        get() = asset

    override val stateAwareActions: Single<Set<StateAwareAction>>
        get() = if (accounts.isEmpty()) {
            Single.just(emptySet())
        } else {
            Single.zip(accounts.map { it.stateAwareActions }) { t: Array<Any> ->
                t.filterIsInstance<Iterable<StateAwareAction>>()
                    .flatten()
                    .groupBy { it.action }
                    .toMutableMap()
                    .mapValues { (_, values) ->
                        // We have to remove duplicate AssetActions, we give priority to Available and pick the first as fallback
                        values.firstOrNull { it.state == ActionState.Available } ?: values.first()
                    }
                    .values
                    .toSet()
            }
        }

    // if _any_ of the accounts have transactions
    override val hasTransactions: Boolean
        get() = accounts.map { it.hasTransactions }.any { it }

    // Are any of the accounts funded
    override val isFunded: Boolean = accounts.map { it.isFunded }.any { it }
}
