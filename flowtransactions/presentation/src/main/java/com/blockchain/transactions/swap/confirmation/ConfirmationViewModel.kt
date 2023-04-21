package com.blockchain.transactions.swap.confirmation

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.CryptoAccount
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
import com.blockchain.nabu.datamanagers.repositories.swap.SwapTransactionsStore
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.zipOutcomes
import com.blockchain.utils.awaitOutcome
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

sealed interface ConfirmationNavigation : NavigationEvent {
    data class TransactionState(val order: CustodialOrder) : ConfirmationNavigation
}

class ConfirmationViewModel(
    private val sourceAccount: CryptoAccount,
    private val targetAccount: CryptoAccount,
    private val sourceCryptoAmount: CryptoValue,
    private val direction: TransferDirection,
    // TODO(aromano): SWAP temp comment, this is only going to be used for NC->* swaps
    private val secondPassword: String?,

    private val brokerageDataManager: BrokerageDataManager,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val custodialWalletManager: CustodialWalletManager,
    private val swapTransactionsStore: SwapTransactionsStore,
    private val tradingStore: TradingStore
) : MviViewModel<
    ConfirmationIntent,
    ConfirmationViewState,
    ConfirmationModelState,
    ConfirmationNavigation,
    ModelConfigArgs.NoArgs
    >(
    ConfirmationModelState(
        sourceAccount = sourceAccount,
        targetAccount = targetAccount,
        sourceCryptoAmount = sourceCryptoAmount,
    )
) {

    private var targetFiatRateJob: Job? = null
    private var quoteRefreshingJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
        // Convert Source Crypto Amount to Fiat
        viewModelScope.launch {
            exchangeRatesDataManager.exchangeRateToUserFiatFlow(sourceAccount.currency)
                .doOnData { rate ->
                    updateState {
                        it.copy(sourceFiatAmount = rate.convert(sourceCryptoAmount) as FiatValue)
                    }
                }
                .collect()
        }
        startQuoteRefreshing()
    }

    private fun startQuoteRefreshing() {
        quoteRefreshingJob = viewModelScope.launch {
            val thisScope = this
            var secondsUntilQuoteRefresh = 0
            // Looping to update the quote refresh timer and to fetch another quote when the current expires
            while (true) {
                if (secondsUntilQuoteRefresh <= 0) {
                    updateState { it.copy(isFetchQuoteLoading = true) }
                    val pair = CurrencyPair(sourceAccount.currency, targetAccount.currency)
                    brokerageDataManager.getSwapQuote(pair, sourceCryptoAmount, direction)
                        .doOnSuccess { quote ->
                            val targetCryptoAmount = quote.resultAmount as CryptoValue
                            secondsUntilQuoteRefresh = (quote.expiresAt - System.currentTimeMillis()).toSeconds()
                                .coerceIn(0, 90)
                                .toInt()
                            startUpdatingTargetFiatAmount(targetCryptoAmount)
                            updateState {
                                it.copy(
                                    isFetchQuoteLoading = false,
                                    targetCryptoAmount = targetCryptoAmount,
                                    sourceToTargetExchangeRate = ExchangeRate(
                                        rate = quote.rawPrice.toBigDecimal(),
                                        from = sourceAccount.currency,
                                        to = targetAccount.currency,
                                    ),
                                )
                            }
                        }
                        .doOnFailure { error ->
                            updateState {
                                it.copy(
                                    isFetchQuoteLoading = false,
                                    quoteError = error.toConfirmationError()
                                )
                            }
                            // Quote errors are terminal, we'll show an UxError with actions or a
                            // regular error which will navigate the user out of the Swap flow
                            thisScope.cancel()
                            // TODO(aromano): SWAP check if UxError is still working
                        }
                }
                updateState { it.copy(quoteRefreshRemainingSeconds = secondsUntilQuoteRefresh) }
                delay(1_000)
                secondsUntilQuoteRefresh--
            }
        }
    }

    private fun startUpdatingTargetFiatAmount(amount: CryptoValue) {
        targetFiatRateJob?.cancel()
        targetFiatRateJob = viewModelScope.launch {
            exchangeRatesDataManager.exchangeRateToUserFiatFlow(targetAccount.currency)
                .doOnData { rate ->
                    updateState {
                        it.copy(targetFiatAmount = rate.convert(amount) as FiatValue)
                    }
                }
                .collect()
        }
    }

    override fun reduce(state: ConfirmationModelState): ConfirmationViewState = ConfirmationViewState(
        isFetchQuoteLoading = state.isFetchQuoteLoading,
        sourceAsset = state.sourceAccount.currency,
        targetAsset = state.targetAccount.currency,
        sourceCryptoAmount = state.sourceCryptoAmount,
        sourceFiatAmount = state.sourceFiatAmount,
        targetCryptoAmount = state.targetCryptoAmount,
        targetFiatAmount = state.targetFiatAmount,
        sourceToTargetExchangeRate = state.sourceToTargetExchangeRate,
        quoteRefreshRemainingPercentage = safeLet(
            state.quoteRefreshRemainingSeconds,
            state.quoteRefreshTotalSeconds,
        ) { remaining, total ->
            remaining.toFloat() / total.toFloat()
        },
        quoteRefreshRemainingSeconds = state.quoteRefreshRemainingSeconds,
        submitButtonState = when {
            state.isFetchQuoteLoading -> ButtonState.Disabled
            state.isSubmittingOrderLoading -> ButtonState.Loading
            else -> ButtonState.Enabled
        },
        quoteError = state.quoteError,
        createOrderError = state.createOrderError,
    )

    override suspend fun handleIntent(modelState: ConfirmationModelState, intent: ConfirmationIntent) {
        when (intent) {
            ConfirmationIntent.SubmitClicked -> {
                val quoteId = modelState.quoteId!!

                // NC->NC
                val requiresDestinationAddress = direction == TransferDirection.ON_CHAIN
                // NC->NC or NC->C
                val requireRefundAddress =
                    direction == TransferDirection.ON_CHAIN || direction == TransferDirection.FROM_USERKEY

                // TODO(aromano): SWAP
//                if (requireSecondPassword && secondPassword.isEmpty()) {
//                    throw IllegalArgumentException("Second password not supplied")
//                }

                zipOutcomes(
                    sourceAccount.receiveAddress::awaitOutcome,
                    targetAccount.receiveAddress::awaitOutcome,
                ).flatMap { (sourceAddress, targetAddress) ->
                    custodialWalletManager.createCustodialOrder(
                        direction = direction,
                        quoteId = quoteId,
                        volume = modelState.sourceCryptoAmount,
                        destinationAddress = if (requiresDestinationAddress) targetAddress.address else null,
                        refundAddress = if (requireRefundAddress) sourceAddress.address else null,
                    ).awaitOutcome()
                }.doOnSuccess { order ->
                    quoteRefreshingJob?.cancel()
                    swapTransactionsStore.invalidate()
                    tradingStore.invalidate()
                    // TODO(aromano): SWAP ANALYTICS
//                    analyticsHooks.onTransactionSuccess(newState)
                    navigate(ConfirmationNavigation.TransactionState(order))
                }.doOnFailure { error ->
                    updateState {
                        it.copy(createOrderError = error.toConfirmationError())
                    }
                }
            }
        }
    }
}
