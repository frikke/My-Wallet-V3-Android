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
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.BuySellOrder
import com.blockchain.nabu.datamanagers.CryptoTransaction
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.datamanagers.CustodialOrderState
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.TransactionState
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.nabu.datamanagers.custodialwalletimpl.OrderType
import com.blockchain.nabu.datamanagers.toRecurringBuyFailureReason
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.balance.asAssetInfoOrThrow
import info.blockchain.balance.asFiatCurrencyOrThrow
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.atomic.AtomicBoolean
import piuk.blockchain.androidcore.utils.extensions.mapList

class CustodialTradingAccount(
    override val currency: AssetInfo,
    override val label: String,
    override val exchangeRates: ExchangeRatesDataManager,
    val custodialWalletManager: CustodialWalletManager,
    val tradingBalances: TradingBalanceDataManager,
    private val identity: UserIdentity,
    override val baseActions: Set<AssetAction> = defaultActions
) : CryptoAccountBase(), TradingAccount {

    private val hasFunds = AtomicBoolean(false)

    override val receiveAddress: Single<ReceiveAddress>
        get() = custodialWalletManager.getCustodialAccountAddress(currency).map {
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

    override fun requireSecondPassword(): Single<Boolean> =
        Single.just(false)

    override fun matches(other: CryptoAccount): Boolean =
        other is CustodialTradingAccount && other.currency == currency

    override val balance: Observable<AccountBalance>
        get() = Observable.combineLatest(
            tradingBalances.getBalanceForCurrency(currency),
            exchangeRates.exchangeRateToUserFiat(currency)
        ) { balance, rate ->
            setHasTransactions(balance.hasTransactions)
            AccountBalance.from(balance, rate)
        }.doOnNext { hasFunds.set(it.total.isPositive) }

    override val activity: Single<ActivitySummaryList>
        get() = custodialWalletManager.getAllOrdersFor(currency)
            .mapList { orderToSummary(it) }
            .flatMap { buySellList ->
                appendTradeActivity(custodialWalletManager, currency, buySellList)
            }
            .flatMap {
                appendTransferActivity(custodialWalletManager, currency, it)
            }.filterActivityStates()
            .doOnSuccess { setHasTransactions(it.isNotEmpty()) }
            .onErrorReturn {
                emptyList()
            }

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val isDefault: Boolean =
        false // Default is, presently, only ever a non-custodial account.

    override val sourceState: Single<TxSourceState>
        get() = balance.firstOrError().map { balance ->
            when {
                balance.total <= Money.zero(currency) -> TxSourceState.NO_FUNDS
                balance.withdrawable <= Money.zero(currency) -> TxSourceState.FUNDS_LOCKED
                else -> TxSourceState.CAN_TRANSACT
            }
        }

    override val stateAwareActions: Single<Set<StateAwareAction>>
        get() = Single.zip(
            balance.firstOrError(),
            identity.userAccessForFeatures(
                listOf(
                    Feature.Buy,
                    Feature.Swap,
                    Feature.Sell,
                    Feature.DepositCrypto,
                    Feature.DepositInterest
                )
            ),
            identity.isEligibleFor(Feature.Interest(currency)),
            custodialWalletManager.getSupportedBuySellCryptoCurrencies(),
            custodialWalletManager.getSupportedFundsFiats().onErrorReturn { emptyList() },
            custodialWalletManager.isAssetSupportedForSwap(currency),
            identity.getHighestApprovedKycTier()
        ) { balance, accessToFeatures, isEligibleForInterest, supportedCurrencyPairs, fiatAccounts,
            isAssetSupportedForSwap, highestTier ->

            val buyEligibility = accessToFeatures[Feature.Buy]!!
            val swapEligibility = accessToFeatures[Feature.Swap]!!
            val sellEligibility = accessToFeatures[Feature.Sell]!!
            val depositCryptoEligibility = accessToFeatures[Feature.DepositCrypto]!!
            val depositInterestEligibility = accessToFeatures[Feature.DepositInterest]!!

            val isActiveFunded = !isArchived && balance.total.isPositive

            val activity = StateAwareAction(
                when {
                    highestTier == Tier.BRONZE -> ActionState.LockedForTier
                    baseActions.contains(AssetAction.ViewActivity) -> ActionState.Available
                    else -> ActionState.Unavailable
                },
                AssetAction.ViewActivity
            )

            val receive = StateAwareAction(
                when {
                    !baseActions.contains(AssetAction.Receive) -> ActionState.Unavailable
                    depositCryptoEligibility is FeatureAccess.Blocked -> depositCryptoEligibility.toActionState()
                    else -> ActionState.Available
                },
                AssetAction.Receive
            )

            val buy = StateAwareAction(
                when {
                    !baseActions.contains(AssetAction.Buy) -> ActionState.Unavailable
                    supportedCurrencyPairs.none { it.source == currency } -> ActionState.Unavailable
                    buyEligibility is FeatureAccess.Blocked -> buyEligibility.toActionState()
                    else -> ActionState.Available
                },
                AssetAction.Buy
            )

            val send = StateAwareAction(
                when {
                    !baseActions.contains(AssetAction.Send) -> ActionState.Unavailable
                    isActiveFunded && balance.withdrawable.isPositive -> ActionState.Available
                    else -> ActionState.LockedForBalance
                },
                AssetAction.Send
            )

            val interest = StateAwareAction(
                when {
                    depositInterestEligibility is FeatureAccess.Blocked -> depositInterestEligibility.toActionState()
                    !baseActions.contains(AssetAction.InterestDeposit) -> ActionState.Unavailable
                    isActiveFunded && isEligibleForInterest -> ActionState.Available
                    !isEligibleForInterest -> ActionState.LockedForTier
                    !isActiveFunded -> ActionState.LockedForBalance
                    else -> ActionState.Unavailable
                },
                AssetAction.InterestDeposit
            )

            val swap = StateAwareAction(
                when {
                    !baseActions.contains(AssetAction.Swap) -> ActionState.Unavailable
                    !isAssetSupportedForSwap -> ActionState.Unavailable
                    swapEligibility is FeatureAccess.Blocked -> swapEligibility.toActionState()
                    !isActiveFunded || balance.withdrawable.isZero -> ActionState.LockedForBalance
                    else -> ActionState.Available
                },
                AssetAction.Swap
            )

            val sell = StateAwareAction(
                when {
                    !baseActions.contains(AssetAction.Sell) -> ActionState.Unavailable
                    sellEligibility is FeatureAccess.Blocked -> sellEligibility.toActionState()
                    fiatAccounts.isEmpty() -> ActionState.LockedForTier
                    !isActiveFunded -> ActionState.LockedForBalance
                    else -> ActionState.Available
                },
                AssetAction.Sell
            )

            setOf(
                buy, sell, swap, send, receive, interest, activity
            )
        }

    override val hasStaticAddress: Boolean = false

    private fun appendTransferActivity(
        custodialWalletManager: CustodialWalletManager,
        asset: AssetInfo,
        summaryList: List<ActivitySummaryItem>
    ) = custodialWalletManager.getCustodialCryptoTransactions(asset, Product.BUY)
        .map { txs ->
            txs.map {
                it.toSummaryItem()
            } + summaryList
        }

    private fun CryptoTransaction.toSummaryItem() =
        CustodialTransferActivitySummaryItem(
            asset = this@CustodialTradingAccount.currency,
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
                    asset = order.target.currency.asAssetInfoOrThrow(),
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
                    asset = order.target.currency.asAssetInfoOrThrow(),
                    value = order.target,
                    fundedFiat = order.source,
                    price = order.price,
                    txId = order.id,
                    timeStampMs = order.created.time,
                    status = order.state,
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
                        order.source.currency, order.target.currency
                    ),
                    fiatValue = order.target,
                    fiatCurrency = order.target.currency.asFiatCurrencyOrThrow()
                )
            }
        }

    // Stop gap filter, until we finalise which item we wish to display to the user.
    // TODO: This can be done via the API when it's settled
    private fun Single<ActivitySummaryList>.filterActivityStates(): Single<ActivitySummaryList> {
        return flattenAsObservable { list ->
            list.filter {
                (
                    it is CustodialTradingActivitySummaryItem && displayedStates.contains(
                        it.status
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
        }.toList()
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
