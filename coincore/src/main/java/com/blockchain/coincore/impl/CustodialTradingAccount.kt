package com.blockchain.coincore.impl

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CustodialTradingActivitySummaryItem
import com.blockchain.coincore.CustodialTransferActivitySummaryItem
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.RecurringBuyActivitySummaryItem
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.TradeActivitySummaryItem
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.TxSourceState
import com.blockchain.coincore.toActionState
import com.blockchain.coincore.toFiat
import com.blockchain.core.custodial.domain.TradingService
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.dataOrNull
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.BuySellOrder
import com.blockchain.nabu.datamanagers.CryptoTransaction
import com.blockchain.nabu.datamanagers.CustodialOrderState
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.TransactionState
import com.blockchain.nabu.datamanagers.custodialwalletimpl.OrderType
import com.blockchain.nabu.datamanagers.toRecurringBuyFailureReason
import com.blockchain.utils.zipSingles
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.balance.asAssetInfoOrThrow
import info.blockchain.balance.asFiatCurrencyOrThrow
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.rx3.asObservable
import kotlinx.coroutines.rx3.rxSingle

class CustodialTradingAccount(
    override val currency: AssetInfo,
    override val label: String,
    override val exchangeRates: ExchangeRatesDataManager,
    val custodialWalletManager: CustodialWalletManager,
    private val tradingService: TradingService,
    private val identity: UserIdentity,
    private val kycService: KycService,
    private val walletModeService: WalletModeService
) : CryptoAccountBase(), TradingAccount {

    override val baseActions: Single<Set<AssetAction>>
        get() = walletModeService.walletModeSingle.map { wMode ->
            when (wMode) {
                WalletMode.NON_CUSTODIAL -> emptySet()
                WalletMode.CUSTODIAL -> defaultCustodialActions
            }
        }

    override val receiveAddress: Single<ReceiveAddress>
        get() = custodialWalletManager.getCustodialAccountAddress(Product.BUY, currency).map {
            makeExternalAssetAddress(
                asset = currency,
                address = it,
                label = label,
                postTransactions = onTxCompleted
            )
        }

    override val directions: Set<TransferDirection> = setOf(TransferDirection.INTERNAL)

    override val onTxCompleted: (TxResult) -> Completable
        get() = { txResult ->
            receiveAddress.flatMapCompletable {
                require(txResult.amount is CryptoValue)
                require(txResult is TxResult.HashedTxResult)
                custodialWalletManager.createPendingDeposit(
                    crypto = txResult.amount.currency,
                    address = it.address,
                    hash = txResult.txId,
                    amount = txResult.amount,
                    product = Product.BUY
                )
            }
        }

    override fun matches(other: CryptoAccount): Boolean =
        other is CustodialTradingAccount && other.currency == currency

    override fun balanceRx(freshnessStrategy: FreshnessStrategy): Observable<AccountBalance> =
        Observable.combineLatest(
            tradingService.getBalanceFor(
                asset = currency,
                refreshStrategy = freshnessStrategy
            ),
            exchangeRates.exchangeRateToUserFiatFlow(currency).asObservable()
        ) { balance, rate ->
            AccountBalance(
                total = balance.total,
                withdrawable = balance.withdrawable,
                pending = balance.pending,
                exchangeRate = rate.dataOrNull()
            )
        }

    override fun activity(freshnessStrategy: FreshnessStrategy): Observable<ActivitySummaryList> {
        return custodialWalletManager.getAllOrdersFor(freshnessStrategy, currency)
            .map { list -> list.map { orderToSummary(it) } }
            .flatMap { buySellList ->
                appendTradeActivity(custodialWalletManager, currency, buySellList, freshnessStrategy)
            }
            .flatMap {
                appendTransferActivity(freshnessStrategy, custodialWalletManager, currency, it)
            }.map {
                it.filterActivityStates()
            }
            .onErrorReturn {
                emptyList()
            }
    }

    override val isDefault: Boolean =
        false // Default is, presently, only ever a non-custodial account.

    override val sourceState: Single<TxSourceState>
        get() = balanceRx().firstOrError().map { balance ->
            when {
                balance.total <= Money.zero(currency) -> TxSourceState.NO_FUNDS
                balance.withdrawable <= Money.zero(currency) -> TxSourceState.FUNDS_LOCKED
                else -> TxSourceState.CAN_TRANSACT
            }
        }

    override val stateAwareActions: Single<Set<StateAwareAction>>
        get() = baseActions.flatMap { actions ->
            actions.map { it.eligibility() }.zipSingles()
                .map { it.toSet() }
        }

    override fun stateOfAction(assetAction: AssetAction): Single<ActionState> {
        return assetAction.eligibility().map {
            it.state
        }
    }

    private fun AssetAction.eligibility(): Single<StateAwareAction> =
        when (this) {
            AssetAction.ViewActivity -> viewActivityEligibility()
            AssetAction.Receive -> receiveEligibility()
            AssetAction.Send -> balanceRx().firstOrError().flatMap { sendEligibility(it) }
            AssetAction.InterestDeposit -> balanceRx().firstOrError().flatMap { interestDepositEligibility(it) }
            AssetAction.Swap -> balanceRx().firstOrError().flatMap { swapEligibility(it) }
            AssetAction.Sell -> balanceRx().firstOrError().flatMap { sellEligibility(it) }
            AssetAction.Buy -> buyEligibility()
            AssetAction.StakingDeposit -> balanceRx().firstOrError().flatMap { stakingDepositEligibility(it) }
            AssetAction.ActiveRewardsDeposit -> balanceRx().firstOrError()
                .flatMap { activeRewardsDepositEligibility(it) }

            AssetAction.ViewStatement,
            AssetAction.FiatWithdraw,
            AssetAction.InterestWithdraw,
            AssetAction.FiatDeposit,
            AssetAction.ActiveRewardsWithdraw,
            AssetAction.StakingWithdraw,
            AssetAction.Sign -> Single.just(StateAwareAction(ActionState.Unavailable, this))
        }

    private fun buyEligibility(): Single<StateAwareAction> {
        val supportedPairs = custodialWalletManager.getSupportedBuySellCryptoCurrencies()
        val buyEligibility = identity.userAccessForFeature(Feature.Buy, defFreshness)

        return supportedPairs.onErrorReturn { emptyList() }.zipWith(buyEligibility) { pairs, buyEligible ->
            StateAwareAction(
                when {
                    pairs.none { it.source == currency } -> ActionState.Unavailable
                    buyEligible is FeatureAccess.Blocked -> buyEligible.toActionState()
                    else -> ActionState.Available
                },
                AssetAction.Buy
            )
        }
    }

    private fun sellEligibility(balance: AccountBalance): Single<StateAwareAction> {
        val accountsFiat = rxSingle {
            custodialWalletManager.getSupportedFundsFiats(
                freshnessStrategy =
                FreshnessStrategy.Cached(RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES))
            ).first()
        }
            .onErrorReturn { emptyList() }

        val sellEligibility = identity.userAccessForFeature(Feature.Sell, defFreshness)

        val isAvailableForTrading = custodialWalletManager.isCurrencyAvailableForTradingLegacy(assetInfo = currency)

        return Single.zip(
            sellEligibility,
            accountsFiat,
            isAvailableForTrading
        ) { sellEligible, fiatAccounts, isTradable ->
            StateAwareAction(
                state = when {
                    sellEligible is FeatureAccess.Blocked -> sellEligible.toActionState()
                    fiatAccounts.isEmpty() -> ActionState.LockedForTier
                    balance.total.isPositive.not() -> ActionState.LockedForBalance
                    isTradable.not() -> ActionState.LockedDueToAvailability
                    else -> ActionState.Available
                },
                action = AssetAction.Sell
            )
        }
    }

    private fun swapEligibility(balance: AccountBalance): Single<StateAwareAction> {
        val assetAvailable = custodialWalletManager.isAssetSupportedForSwap(assetInfo = currency)
        val swapEligibility = identity.userAccessForFeature(Feature.Swap, defFreshness)
        return swapEligibility.zipWith(assetAvailable) { swapEligible, isAssetAvailable ->
            StateAwareAction(
                when {
                    !isAssetAvailable -> ActionState.Unavailable
                    swapEligible is FeatureAccess.Blocked -> swapEligible.toActionState()
                    swapEligible is FeatureAccess.Blocked -> swapEligible.toActionState()
                    balance.withdrawable.isZero -> ActionState.LockedForBalance
                    else -> ActionState.Available
                },
                AssetAction.Swap
            )
        }
    }

    private fun interestDepositEligibility(balance: AccountBalance): Single<StateAwareAction> {
        val depositInterestEligibility = identity.userAccessForFeature(Feature.DepositInterest, defFreshness)
        val currencyInterestEligibility = identity.isEligibleFor(Feature.Interest(currency), defFreshness)

        return depositInterestEligibility.zipWith(
            currencyInterestEligibility
        ) { depositInterestEligible, currencyInterestEligible ->
            StateAwareAction(
                when {
                    depositInterestEligible is FeatureAccess.Blocked -> depositInterestEligible.toActionState()
                    !currencyInterestEligible -> ActionState.LockedForTier
                    balance.total.isPositive.not() -> ActionState.LockedForBalance
                    else -> ActionState.Available
                },
                AssetAction.InterestDeposit
            )
        }
    }

    private fun stakingDepositEligibility(balance: AccountBalance): Single<StateAwareAction> =
        identity.userAccessForFeature(Feature.DepositStaking, defFreshness).map { access ->
            StateAwareAction(
                when {
                    access is FeatureAccess.Blocked -> access.toActionState()
                    balance.total.isPositive.not() -> ActionState.LockedForBalance
                    else -> ActionState.Available
                },
                AssetAction.StakingDeposit
            )
        }

    private fun activeRewardsDepositEligibility(balance: AccountBalance): Single<StateAwareAction> =
        identity.userAccessForFeature(Feature.DepositActiveRewards, defFreshness).map { access ->
            StateAwareAction(
                when {
                    access is FeatureAccess.Blocked -> access.toActionState()
                    balance.total.isPositive.not() -> ActionState.LockedForBalance
                    else -> ActionState.Available
                },
                AssetAction.ActiveRewardsDeposit
            )
        }

    private fun sendEligibility(balance: AccountBalance): Single<StateAwareAction> {
        return Single.just(
            StateAwareAction(
                if (balance.withdrawable.isPositive) {
                    ActionState.Available
                } else ActionState.LockedForBalance,
                AssetAction.Send
            )
        )
    }

    private fun receiveEligibility(): Single<StateAwareAction> {
        val depositCryptoEligibility = identity.userAccessForFeature(Feature.DepositCrypto, defFreshness)
        return depositCryptoEligibility.map { access ->
            StateAwareAction(
                if (access is FeatureAccess.Blocked) {
                    access.toActionState()
                } else ActionState.Available,
                AssetAction.Receive
            )
        }
    }

    private fun viewActivityEligibility(): Single<StateAwareAction> {
        val tier = kycService.getHighestApprovedTierLevelLegacy()
        return tier.map { highestTier ->
            StateAwareAction(
                when (highestTier) {
                    KycTier.BRONZE -> ActionState.LockedForTier
                    else -> ActionState.Available
                },
                AssetAction.ViewActivity
            )
        }
    }

    override val hasStaticAddress: Boolean = false

    private fun appendTransferActivity(
        freshnessStrategy: FreshnessStrategy,
        custodialWalletManager: CustodialWalletManager,
        asset: AssetInfo,
        summaryList: List<ActivitySummaryItem>
    ) = custodialWalletManager.getCustodialCryptoTransactions(freshnessStrategy, asset, Product.BUY)
        .map { txs ->
            txs.map {
                it.toSummaryItem()
            } + summaryList
        }

    private fun CryptoTransaction.toSummaryItem() =
        CustodialTransferActivitySummaryItem(
            currency = this@CustodialTradingAccount.currency,
            exchangeRates = exchangeRates,
            txId = id,
            timeStampMs = date.time,
            value = amount,
            account = this@CustodialTradingAccount,
            fee = fee,
            recipientAddress = receivingAddress,
            txHash = txHash,
            state = state,
            type = type,
            fiatValue = amount.toFiat(currency, exchangeRates) as FiatValue,
            paymentMethodId = paymentId
        )

    private fun orderToSummary(order: BuySellOrder): ActivitySummaryItem =
        when (order.type) {
            OrderType.RECURRING_BUY -> {
                RecurringBuyActivitySummaryItem(
                    exchangeRates = exchangeRates,
                    currency = order.target.currency.asAssetInfoOrThrow(),
                    value = order.target,
                    fundedFiat = order.source,
                    txId = order.id,
                    timeStampMs = order.created.time,
                    transactionState = order.state,
                    fee = (order.fee ?: Money.zero(order.source.currency)),
                    account = this,
                    type = order.type,
                    paymentMethodId = order.paymentMethodId,
                    paymentMethodType = order.paymentMethodType,
                    failureReason = order.failureReason.toRecurringBuyFailureReason(),
                    recurringBuyId = order.recurringBuyId
                )
            }

            OrderType.BUY -> {
                CustodialTradingActivitySummaryItem(
                    exchangeRates = exchangeRates,
                    currency = order.target.currency.asAssetInfoOrThrow(),
                    value = order.target,
                    fundedFiat = order.source,
                    price = order.price,
                    txId = order.id,
                    timeStampMs = order.created.time,
                    state = order.state,
                    fee = (order.fee ?: Money.zero(order.source.currency)),
                    account = this,
                    type = order.type,
                    paymentMethodId = order.paymentMethodId,
                    paymentMethodType = order.paymentMethodType,
                    depositPaymentId = order.depositPaymentId
                )
            }

            OrderType.SELL -> {
                TradeActivitySummaryItem(
                    exchangeRates = exchangeRates,
                    txId = order.id,
                    timeStampMs = order.created.time,
                    sendingValue = order.target,
                    price = order.price,
                    sendingAccount = this,
                    sendingAddress = null,
                    receivingAddress = null,
                    state = order.state.toCustodialOrderState(),
                    direction = TransferDirection.INTERNAL,
                    receivingValue = order.orderValue ?: throw IllegalStateException(
                        "Order missing receivingValue"
                    ),
                    depositNetworkFee = Single.just(Money.zero(order.target.currency)),
                    withdrawalNetworkFee = order.fee ?: Money.zero(order.source.currency),
                    currencyPair = CurrencyPair(
                        order.source.currency,
                        order.target.currency
                    ),
                    fiatValue = order.target,
                    currency = order.target.currency.asFiatCurrencyOrThrow()
                )
            }
        }

    // Stop gap filter, until we finalise which item we wish to display to the user.
    // TODO: This can be done via the API when it's settled
    private fun ActivitySummaryList.filterActivityStates(): ActivitySummaryList {
        return this.filter {
            (
                it is CustodialTradingActivitySummaryItem && displayedStates.contains(
                    it.state
                )
                ) or (
                it is CustodialTransferActivitySummaryItem && displayedStates.contains(
                    it.state
                )
                ) or (
                it is TradeActivitySummaryItem && displayedStates.contains(
                    it.state
                )
                ) or (it is RecurringBuyActivitySummaryItem)
        }
    }

    // No need to reconcile sends and swaps in custodial accounts, the BE deals with this
    // Return a list containing both supplied list
    override fun reconcileSwaps(
        tradeItems: List<TradeActivitySummaryItem>,
        activity: List<ActivitySummaryItem>
    ): List<ActivitySummaryItem> = activity + tradeItems

    companion object {
        private val displayedStates = setOf(
            OrderState.FINISHED,
            OrderState.AWAITING_FUNDS,
            OrderState.PENDING_EXECUTION,
            OrderState.FAILED,
            CustodialOrderState.FINISHED,
            TransactionState.COMPLETED,
            TransactionState.MANUAL_REVIEW,
            TransactionState.PENDING,
            CustodialOrderState.PENDING_DEPOSIT,
            CustodialOrderState.PENDING_EXECUTION,
            CustodialOrderState.FAILED
        )
    }
}

private fun OrderState.toCustodialOrderState(): CustodialOrderState =
    when (this) {
        OrderState.FINISHED -> CustodialOrderState.FINISHED
        OrderState.CANCELED -> CustodialOrderState.CANCELED
        OrderState.FAILED -> CustodialOrderState.FAILED
        OrderState.PENDING_CONFIRMATION -> CustodialOrderState.PENDING_CONFIRMATION
        OrderState.AWAITING_FUNDS -> CustodialOrderState.PENDING_DEPOSIT
        OrderState.PENDING_EXECUTION -> CustodialOrderState.PENDING_EXECUTION
        else -> CustodialOrderState.UNKNOWN
    }
