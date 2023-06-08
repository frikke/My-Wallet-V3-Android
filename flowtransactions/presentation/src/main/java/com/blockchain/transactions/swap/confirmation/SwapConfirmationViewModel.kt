package com.blockchain.transactions.swap.confirmation

import androidx.lifecycle.viewModelScope
import com.blockchain.betternavigation.utils.Bindable
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
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
import com.blockchain.core.custodial.BrokerageDataManager
import com.blockchain.core.custodial.data.store.TradingStore
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.doOnData
import com.blockchain.domain.common.model.toSeconds
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.extensions.safeLet
import com.blockchain.nabu.datamanagers.CustodialOrder
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.repositories.swap.SwapTransactionsStore
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.outcome.zipOutcomes
import com.blockchain.transactions.swap.neworderstate.composable.SwapNewOrderState
import com.blockchain.transactions.swap.neworderstate.composable.SwapNewOrderStateArgs
import com.blockchain.utils.awaitOutcome
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Completable
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

sealed interface SwapConfirmationNavigation : NavigationEvent {
    data class NewOrderState(val args: SwapNewOrderStateArgs) : SwapConfirmationNavigation
}

class SwapConfirmationViewModel(
    private val args: SwapConfirmationArgs,
    private val brokerageDataManager: BrokerageDataManager,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val custodialWalletManager: CustodialWalletManager,
    private val swapTransactionsStore: SwapTransactionsStore,
    private val tradingStore: TradingStore
) : MviViewModel<
    SwapConfirmationIntent,
    SwapConfirmationViewState,
    SwapConfirmationModelState,
    SwapConfirmationNavigation,
    ModelConfigArgs.NoArgs
    >(
    SwapConfirmationModelState()
) {
    private val sourceAccount: CryptoAccount
        get() = args.sourceAccount.data!!
    private val targetAccount: CryptoAccount
        get() = args.targetAccount.data!!
    private val sourceCryptoAmount: CryptoValue = args.sourceCryptoAmount
    private val secondPassword: String? = args.secondPassword

    private var quoteRefreshingJob: Job? = null

    private lateinit var depositTxEngine: OnChainTxEngineBase
    private lateinit var depositPendingTx: PendingTx

    init {
        @Suppress("UNNECESSARY_SAFE_CALL") // Because of KoinGraphTest
        if (args?.sourceAccount?.data != null && args?.targetAccount?.data != null) {
            // Convert Source Crypto Amount to Fiat
            viewModelScope.launch {
                exchangeRatesDataManager.exchangeRateToUserFiatFlow(sourceAccount.currency)
                    .doOnData { rate ->
                        updateState {
                            copy(sourceToFiatExchangeRate = rate)
                        }
                    }
                    .collect()
            }

            // Convert Target Crypto Amount to Fiat
            viewModelScope.launch {
                exchangeRatesDataManager.exchangeRateToUserFiatFlow(targetAccount.currency)
                    .doOnData { rate ->
                        updateState { copy(targetToFiatExchangeRate = rate) }
                    }
                    .collect()
            }

            startQuoteRefreshing()

            viewModelScope.launch {
                val sourceAccount = sourceAccount
                if (sourceAccount is CryptoNonCustodialAccount) {
                    updateState { copy(isStartingDepositOnChainTxEngine = true) }
                    depositTxEngine =
                        sourceAccount.createTxEngine(targetAccount, AssetAction.Swap) as OnChainTxEngineBase
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
                            navigate(SwapConfirmationNavigation.NewOrderState(error.toNewOrderStateArgs()))
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
                        .getSwapQuote(
                            pair = pair,
                            amount = sourceCryptoAmount,
                            direction = transferDirection
                        )
                        .doOnSuccess { quote ->
                            val targetCryptoAmount = quote.resultAmount as CryptoValue
                            secondsUntilQuoteRefresh = (quote.expiresAt - System.currentTimeMillis()).toSeconds()
                                .coerceIn(0, 90)
                                .toInt()
                            updateState {
                                copy(
                                    quoteId = quote.id,
                                    isFetchQuoteLoading = false,
                                    targetCryptoAmount = targetCryptoAmount,
                                    quoteRefreshTotalSeconds = secondsUntilQuoteRefresh,
                                    sourceToTargetExchangeRate = ExchangeRate(
                                        rate = quote.rawPrice.toBigDecimal(),
                                        from = sourceAccount.currency,
                                        to = targetAccount.currency
                                    ),
                                    targetNetworkFeeCryptoAmount = quote.networkFee as CryptoValue?
                                )
                            }
                        }
                        .doOnFailure { error ->
                            navigate(SwapConfirmationNavigation.NewOrderState(error.toNewOrderStateArgs()))
                            // Quote errors are terminal, we'll show an UxError with actions or a
                            // regular error which will navigate the user out of the Swap flow
                            thisScope.cancel()
                        }
                }
                updateState { copy(quoteRefreshRemainingSeconds = secondsUntilQuoteRefresh) }
                delay(1_000)
                secondsUntilQuoteRefresh--
            }
        }
    }

    override fun SwapConfirmationModelState.reduce(): SwapConfirmationViewState = SwapConfirmationViewState(
        isFetchQuoteLoading = isFetchQuoteLoading,
        sourceAsset = sourceAccount.currency,
        targetAsset = targetAccount.currency,
        sourceCryptoAmount = sourceCryptoAmount,
        sourceFiatAmount = sourceCryptoAmount.toUserFiat(),
        targetCryptoAmount = targetCryptoAmount,
        targetFiatAmount = targetCryptoAmount?.toUserFiat(),
        sourceToTargetExchangeRate = sourceToTargetExchangeRate,
        sourceNetworkFeeCryptoAmount = sourceNetworkFeeCryptoAmount,
        sourceNetworkFeeFiatAmount = sourceNetworkFeeCryptoAmount?.toUserFiat(),
        targetNetworkFeeCryptoAmount = targetNetworkFeeCryptoAmount,
        targetNetworkFeeFiatAmount = targetNetworkFeeCryptoAmount?.toUserFiat(),
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

    override suspend fun handleIntent(modelState: SwapConfirmationModelState, intent: SwapConfirmationIntent) {
        when (intent) {
            SwapConfirmationIntent.SubmitClicked -> {
                updateState { copy(isSubmittingOrderLoading = true) }
                val quoteId = modelState.quoteId!!

                // NC->NC
                val requiresDestinationAddress = transferDirection == TransferDirection.ON_CHAIN
                // NC->NC or NC->C
                val requireRefundAddress = transferDirection == TransferDirection.ON_CHAIN ||
                    transferDirection == TransferDirection.FROM_USERKEY

                zipOutcomes(
                    sourceAccount.receiveAddress::awaitOutcome,
                    targetAccount.receiveAddress::awaitOutcome
                ).flatMap { (sourceAddress, targetAddress) ->
                    custodialWalletManager.createCustodialOrder(
                        direction = transferDirection,
                        quoteId = quoteId,
                        volume = sourceCryptoAmount,
                        destinationAddress = if (requiresDestinationAddress) targetAddress.address else null,
                        refundAddress = if (requireRefundAddress) sourceAddress.address else null
                    ).awaitOutcome()
                }.flatMap { order ->
                    if (sourceAccount is NonCustodialAccount) {
                        submitDepositTx(order)
                    } else Outcome.Success(order)
                }.doOnSuccess { order ->
                    quoteRefreshingJob?.cancel()
                    swapTransactionsStore.invalidate()
                    if (transferDirection == TransferDirection.INTERNAL) {
                        tradingStore.invalidate()
                    }

                    val newOrderStateArgs = SwapNewOrderStateArgs(
                        sourceAmount = order.inputMoney as CryptoValue,
                        targetAmount = order.outputMoney as CryptoValue,
                        targetAccount = Bindable(targetAccount),
                        orderState = if (sourceAccount is NonCustodialAccount) {
                            SwapNewOrderState.PendingDeposit
                        } else {
                            SwapNewOrderState.Succeeded
                        }
                    )
                    navigate(SwapConfirmationNavigation.NewOrderState(newOrderStateArgs))
                }.doOnFailure { error ->
                    navigate(SwapConfirmationNavigation.NewOrderState(error.toNewOrderStateArgs()))
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
        get() = when {
            sourceAccount is NonCustodialAccount &&
                targetAccount is NonCustodialAccount -> {
                TransferDirection.ON_CHAIN
            }

            sourceAccount is NonCustodialAccount -> {
                TransferDirection.FROM_USERKEY
            }
            // TransferDirection.FROM_USERKEY not supported
            targetAccount is NonCustodialAccount -> {
                throw UnsupportedOperationException()
            }

            else -> {
                TransferDirection.INTERNAL
            }
        }

    private fun Exception.toNewOrderStateArgs(): SwapNewOrderStateArgs = SwapNewOrderStateArgs(
        sourceAmount = sourceCryptoAmount,
        targetAmount = modelState.targetCryptoAmount ?: CryptoValue.zero(targetAccount.currency),
        targetAccount = Bindable(targetAccount),
        orderState = SwapNewOrderState.Error(this)
    )

    private fun CryptoValue.toUserFiat(): FiatValue = this.toUserFiat(exchangeRatesDataManager) as FiatValue
}
