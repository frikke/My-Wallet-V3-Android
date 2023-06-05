package com.blockchain.transactions.sell.confirmation

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.makeExternalAssetAddress
import com.blockchain.coincore.impl.txEngine.OnChainTxEngineBase
import com.blockchain.coincore.toUserFiat
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.core.TransactionsStore
import com.blockchain.core.custodial.BrokerageDataManager
import com.blockchain.core.custodial.data.store.TradingStore
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.domain.common.model.toSeconds
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.extensions.safeLet
import com.blockchain.nabu.datamanagers.CustodialOrder
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.repositories.swap.CustodialSwapActivityStore
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.transactions.sell.neworderstate.composable.SellNewOrderState
import com.blockchain.transactions.sell.neworderstate.composable.SellNewOrderStateArgs
import com.blockchain.utils.awaitOutcome
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Completable
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface SellConfirmationNavigation : NavigationEvent {
    data class NewOrderState(val args: SellNewOrderStateArgs) : SellConfirmationNavigation
}

class SellConfirmationViewModel(
    private val args: SellConfirmationArgs,
    private val brokerageDataManager: BrokerageDataManager,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val custodialWalletManager: CustodialWalletManager,
    private val tradingStore: TradingStore,
    private val swapActivityStore: CustodialSwapActivityStore,
    private val transactionsStore: TransactionsStore,
) : MviViewModel<
    SellConfirmationIntent,
    SellConfirmationViewState,
    SellConfirmationModelState,
    SellConfirmationNavigation,
    ModelConfigArgs.NoArgs
    >(
    SellConfirmationModelState()
) {
    private val sourceAccount: CryptoAccount
        get() = args.sourceAccount.data!!
    private val targetAccount: FiatAccount
        get() = args.targetAccount.data!!
    private val sourceCryptoAmount: CryptoValue = args.sourceCryptoAmount
    private val secondPassword: String? = args.secondPassword

    private var quoteRefreshingJob: Job? = null

    private lateinit var depositTxEngine: OnChainTxEngineBase
    private lateinit var depositPendingTx: PendingTx

    init {
        viewModelScope.launch {
            if (args.sourceAccount.data != null && args.targetAccount.data != null) {
                startQuoteRefreshing()

                val sourceAccount = sourceAccount
                if (sourceAccount is CryptoNonCustodialAccount) {
                    updateState { copy(isStartingDepositOnChainTxEngine = true) }
                    depositTxEngine =
                        sourceAccount.createTxEngine(targetAccount, AssetAction.Sell) as OnChainTxEngineBase
                    custodialWalletManager.getCustodialAccountAddress(Product.TRADE, sourceAccount.currency)
                        .awaitOutcome()
                        .flatMap { sampleDepositAddress ->
                            depositTxEngine.start(
                                sourceAccount = sourceAccount,
                                txTarget = makeExternalAssetAddress(
                                    asset = sourceAccount.currency,
                                    address = sampleDepositAddress
                                ),
                                exchangeRates = exchangeRatesDataManager
                            )
                            depositTxEngine.doInitialiseTx().awaitOutcome()
                        }.flatMap { pendingTx ->
                            depositTxEngine.doUpdateAmount(sourceCryptoAmount, pendingTx).awaitOutcome()
                        }.doOnSuccess { pendingTx ->
                            depositPendingTx = pendingTx
                            updateState {
                                val sourceFee = pendingTx.feeAmount as? CryptoValue
                                copy(
                                    isStartingDepositOnChainTxEngine = false,
                                    sourceNetworkFeeCryptoAmount = sourceFee
                                )
                            }
                        }.doOnFailure { error ->
                            navigate(SellConfirmationNavigation.NewOrderState(error.toNewOrderStateArgs()))
                            quoteRefreshingJob?.cancel()
                        }
                }
            }
        }
    }

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    private fun startQuoteRefreshing() {
        quoteRefreshingJob = viewModelScope.launch {
            val thisScope = this
            var secondsUntilQuoteRefresh = 0
            // Looping to update the quote refresh timer and to fetch another quote when the current expires
            while (true) {
                if (secondsUntilQuoteRefresh <= 0) {
                    updateState { copy(isFetchQuoteLoading = true) }
                    val pair = CurrencyPair(
                        source = sourceAccount.currency,
                        destination = targetAccount.currency
                    )
                    brokerageDataManager
                        .getSellQuote(
                            pair = pair,
                            amount = sourceCryptoAmount,
                            direction = transferDirection
                        )
                        .doOnSuccess { quote ->
                            val targetFiatAmount = quote.resultAmount as FiatValue
                            secondsUntilQuoteRefresh = (quote.expiresAt - System.currentTimeMillis()).toSeconds()
                                .coerceIn(0, 90)
                                .toInt()
                            updateState {
                                copy(
                                    quoteId = quote.id,
                                    isFetchQuoteLoading = false,
                                    targetFiatAmount = targetFiatAmount,
                                    quoteRefreshTotalSeconds = secondsUntilQuoteRefresh,
                                    sourceToTargetExchangeRate = ExchangeRate(
                                        rate = quote.rawPrice.toBigDecimal(),
                                        from = sourceAccount.currency,
                                        to = targetAccount.currency
                                    ),
                                )
                            }
                        }
                        .doOnFailure { error ->
                            navigate(SellConfirmationNavigation.NewOrderState(error.toNewOrderStateArgs()))
                            // Quote errors are terminal, we'll show an UxError with actions or a
                            // regular error which will navigate the user out of the Sell flow
                            thisScope.cancel()
                        }
                }
                updateState { copy(quoteRefreshRemainingSeconds = secondsUntilQuoteRefresh) }
                delay(1_000)
                secondsUntilQuoteRefresh--
            }
        }
    }

    override fun SellConfirmationModelState.reduce(): SellConfirmationViewState {
        val sourceNetworkFeeFiatAmount = sourceNetworkFeeCryptoAmount?.takeIf { !it.isZero }
            ?.toUserFiat(exchangeRatesDataManager) as FiatValue?

        return SellConfirmationViewState(
            isFetchQuoteLoading = isFetchQuoteLoading,
            sourceAsset = sourceAccount.currency,
            targetAsset = targetAccount.currency,
            sourceCryptoAmount = sourceCryptoAmount,
            targetFiatAmount = targetFiatAmount,
            sourceToTargetExchangeRate = sourceToTargetExchangeRate,
            sourceNetworkFeeFiatAmount = sourceNetworkFeeFiatAmount,
            totalFiatAmount = targetFiatAmount?.let { fiatAmount ->
                val feeAmount = sourceNetworkFeeFiatAmount ?: FiatValue.zero(targetAccount.currency)
                (fiatAmount + feeAmount) as FiatValue
            },
            totalCryptoAmount = let {
                val feeAmount = sourceNetworkFeeCryptoAmount ?: CryptoValue.zero(sourceAccount.currency)
                if (feeAmount.currency == sourceCryptoAmount.currency) {
                    (sourceCryptoAmount + feeAmount) as CryptoValue
                } else {
                    sourceCryptoAmount
                }
            },
            quoteRefreshRemainingPercentage = safeLet(
                quoteRefreshRemainingSeconds,
                quoteRefreshTotalSeconds
            ) { remaining, total ->
                remaining.toFloat() / total.toFloat()
            },
            quoteRefreshRemainingSeconds = quoteRefreshRemainingSeconds,
            submitButtonState = when {
                isFetchQuoteLoading || isStartingDepositOnChainTxEngine -> ButtonState.Disabled
                isSubmittingOrderLoading -> ButtonState.Loading
                else -> ButtonState.Enabled
            }
        )
    }

    override suspend fun handleIntent(modelState: SellConfirmationModelState, intent: SellConfirmationIntent) {
        when (intent) {
            SellConfirmationIntent.SubmitClicked -> {
                updateState { copy(isSubmittingOrderLoading = true) }
                val quoteId = modelState.quoteId!!

                // NC->NC or NC->C
                val requireRefundAddress = transferDirection == TransferDirection.ON_CHAIN ||
                    transferDirection == TransferDirection.FROM_USERKEY

                sourceAccount.receiveAddress.awaitOutcome().flatMap { address ->
                    custodialWalletManager.createCustodialOrder(
                        direction = transferDirection,
                        quoteId = quoteId,
                        volume = sourceCryptoAmount,
                        refundAddress = if (requireRefundAddress) address.address else null
                    ).awaitOutcome()
                }.flatMap { order ->
                    if (sourceAccount is NonCustodialAccount) {
                        submitDepositTx(order)
                    } else Outcome.Success(order)
                }.doOnSuccess { order ->
                    quoteRefreshingJob?.cancel()
                    tradingStore.invalidate()
                    swapActivityStore.invalidate()
                    transactionsStore.invalidate()

                    val newOrderStateArgs = SellNewOrderStateArgs(
                        sourceAmount = order.inputMoney as CryptoValue,
                        targetAmount = order.outputMoney as FiatValue,
                        orderState = if (sourceAccount is NonCustodialAccount) {
                            SellNewOrderState.PendingDeposit
                        } else {
                            SellNewOrderState.Succeeded
                        }
                    )
                    navigate(SellConfirmationNavigation.NewOrderState(newOrderStateArgs))
                }.doOnFailure { error ->
                    navigate(SellConfirmationNavigation.NewOrderState(error.toNewOrderStateArgs()))
                }
                updateState { copy(isSubmittingOrderLoading = false) }
            }
        }
    }

    private suspend fun submitDepositTx(order: CustodialOrder): Outcome<Exception, CustodialOrder> {
        val depositAddress =
            order.depositAddress ?: return Outcome.Failure(IllegalStateException("Missing deposit address"))
        return depositTxEngine.restart(
            txTarget = makeExternalAssetAddress(
                asset = sourceAccount.currency,
                address = depositAddress,
                postTransactions = { Completable.complete() }
            ),
            pendingTx = depositPendingTx
        ).awaitOutcome()
            .flatMap { pendingTx ->
                val depositTxResult = depositTxEngine.doExecute(pendingTx, secondPassword.orEmpty()).awaitOutcome()
                // intentionally ignoring result
                custodialWalletManager.updateOrder(order.id, depositTxResult is Outcome.Success).awaitOutcome()
                depositTxResult.flatMap { txResult ->
                    depositTxEngine.doPostExecute(pendingTx, txResult).awaitOutcome()
                }.doOnSuccess {
                    depositTxEngine.doOnTransactionComplete()
                }.map { order }
            }
    }

    private val transferDirection: TransferDirection
        get() = if (sourceAccount is NonCustodialAccount) {
            TransferDirection.FROM_USERKEY
        } else {
            TransferDirection.INTERNAL
        }

    private fun Exception.toNewOrderStateArgs(): SellNewOrderStateArgs = SellNewOrderStateArgs(
        sourceAmount = sourceCryptoAmount,
        targetAmount = modelState.targetFiatAmount ?: FiatValue.zero(targetAccount.currency),
        orderState = SellNewOrderState.Error(this)
    )
}
