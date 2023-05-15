package com.dex.presentation.confirmation

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.extensions.safeLet
import com.blockchain.preferences.CurrencyPrefs
import com.dex.domain.DexQuote
import com.dex.domain.DexTransaction
import com.dex.domain.DexTransactionProcessor
import com.dex.presentation.uierrors.AlertError
import com.dex.presentation.uierrors.DexUiError
import com.dex.presentation.uierrors.toUiErrors
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DexConfirmationViewModel(
    private val transactionProcessor: DexTransactionProcessor,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val currencyPrefs: CurrencyPrefs
) : MviViewModel<
    ConfirmationIntent,
    ConfirmationScreenViewState,
    ConfirmationModelState,
    ConfirmationNavigationEvent,
    ModelConfigArgs.NoArgs
    >(initialState = ConfirmationModelState(null, null, null, false, emptyList(), null)) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun reduce(state: ConfirmationModelState): ConfirmationScreenViewState {
        val transaction = state.transaction ?: return ConfirmationScreenViewState.Loading
        return ConfirmationScreenViewState.DataConfirmationViewState(
            inputAmount = transaction.amount,
            exchangeInputAmount = safeLet(transaction.amount, state.inputToFiatExchangeRate) { amount, rate ->
                rate.convert(amount)
            } ?: Money.zero(currencyPrefs.selectedFiatCurrency),
            outputAmount = transaction.quote?.outputAmount?.expectedOutput,
            outputExchangeAmount = safeLet(
                transaction.quote?.outputAmount?.expectedOutput,
                state.outputToFiatExchangeRate
            ) { amount, rate ->
                rate.convert(amount)
            } ?: Money.zero(currencyPrefs.selectedFiatCurrency),
            inputCurrency = transaction.sourceAccount.currency,
            outputCurrency = transaction.destinationAccount?.currency,
            inputBalance = transaction.sourceAccount.balance,
            operationInProgress = state.operationInProgress,
            outputBalance = transaction.destinationAccount?.balance,
            dexExchangeRate = transaction.quote?.price?.toBigDecimal()?.setScale(2, RoundingMode.HALF_UP)?.max(
                BigDecimal("0.001")
            ),
            slippage = transaction.slippage,
            minAmount = safeLet(
                transaction.quote?.outputAmount?.minOutputAmount,
                state.outputToFiatExchangeRate
            ) { amount, exchangeRate ->
                ConfirmationScreenExchangeAmount(
                    value = amount,
                    exchange = exchangeRate.convert(amount)
                )
            },
            networkFee = safeLet(
                transaction.quote?.networkFees,
                state.networkFeesToFiatExchangeRate
            ) { amount, exchangeRate ->
                ConfirmationScreenExchangeAmount(
                    value = amount,
                    exchange = exchangeRate.convert(amount)
                )
            },
            blockchainFee = safeLet(
                transaction.quote?.blockchainFees,
                state.outputToFiatExchangeRate
            ) { amount, exchangeRate ->
                ConfirmationScreenExchangeAmount(
                    value = amount,
                    exchange = exchangeRate.convert(amount)
                )
            },
            /*
             * Ignore this error in the confirmation screen
             * */
            errors = transaction.toUiErrors().filter { it != DexUiError.TransactionInProgressError },
            newPriceAvailable = state.priceUpdatedAndNotAccepted
        )
    }

    override suspend fun handleIntent(modelState: ConfirmationModelState, intent: ConfirmationIntent) {
        when (intent) {
            ConfirmationIntent.LoadTransactionData -> loadTransactionData()
            ConfirmationIntent.SubscribeForTxUpdates -> transactionProcessor.subscribeForTxUpdates()
            ConfirmationIntent.UnSubscribeToTxUpdates -> transactionProcessor.unsubscribeToTxUpdates()
            ConfirmationIntent.ConfirmSwap -> {
                executeTx()
            }
            ConfirmationIntent.StopListeningForUpdates -> {
                job?.cancel()
            }
            ConfirmationIntent.AcceptPrice -> {
                updateState { state ->
                    state.copy(
                        priceUpdatedAndNotAccepted = false,
                        acceptedQuoteRates = state.transaction?.quote?.price?.let {
                            state.acceptedQuoteRates.plus(it)
                        } ?: state.acceptedQuoteRates
                    )
                }
            }
        }
    }

    private suspend fun executeTx() {
        updateState {
            it.copy(
                operationInProgress = true
            )
        }
        transactionProcessor.execute()
        updateState {
            it.copy(
                operationInProgress = false
            )
        }
        navigate(ConfirmationNavigationEvent.TxInProgressNavigationEvent)
    }

    var job: Job? = null

    private fun loadTransactionData() {
        job?.cancel()
        job = viewModelScope.launch {
            transactionProcessor.transaction.onEach {
                updateInputFiatExchangeRate(it.sourceAccount.currency, currencyPrefs.selectedFiatCurrency)
                it.quote?.networkFees?.let { fees ->
                    updateNetworkFeesExchangeRate(fees.currency, currencyPrefs.selectedFiatCurrency)
                }
                it.destinationAccount?.currency?.let { currency ->
                    updateOutputFiatExchangeRate(currency, currencyPrefs.selectedFiatCurrency)
                }
            }.collectLatest { tx ->
                updateState { state ->
                    val priceAccepted = tx.quote?.let {
                        priceHasBeenAccepted(it, state.acceptedQuoteRates)
                    } ?: false

                    state.copy(
                        transaction = tx,
                        priceUpdatedAndNotAccepted = !priceAccepted && tx.quote != null,
                        acceptedQuoteRates = if (priceAccepted) {
                            tx.quote?.price?.let {
                                state.acceptedQuoteRates.plus(it)
                            } ?: state.acceptedQuoteRates
                        } else state.acceptedQuoteRates
                    )
                }
            }
        }

        viewModelScope.launch {
            transactionProcessor.quoteFetching.collectLatest {
                updateState { state ->
                    state.copy(
                        operationInProgress = it
                    )
                }
            }
        }
    }

    private fun priceHasBeenAccepted(quote: DexQuote.ExchangeQuote, acceptedQuoteRates: List<Money>): Boolean {
        /*
         * we assume that 1st quote is accepted
         * */
        return acceptedQuoteRates.isEmpty() || acceptedQuoteRates.contains(quote.price)
    }

    private fun updateNetworkFeesExchangeRate(currency: Currency, selectedFiatCurrency: FiatCurrency) {
        viewModelScope.launch {
            exchangeRatesDataManager.exchangeRate(fromAsset = currency, toAsset = selectedFiatCurrency).collectLatest {
                (it as? DataResource.Data)?.data?.let { rate ->
                    updateState { state ->
                        state.copy(
                            networkFeesToFiatExchangeRate = rate
                        )
                    }
                }
            }
        }
    }

    private fun updateInputFiatExchangeRate(from: Currency, to: Currency) {
        viewModelScope.launch {
            exchangeRatesDataManager.exchangeRate(fromAsset = from, toAsset = to).collectLatest {
                (it as? DataResource.Data)?.data?.let { rate ->
                    updateState { state ->
                        state.copy(
                            inputToFiatExchangeRate = rate
                        )
                    }
                }
            }
        }
    }

    private fun updateOutputFiatExchangeRate(from: Currency, to: Currency) {
        viewModelScope.launch {
            exchangeRatesDataManager.exchangeRate(fromAsset = from, toAsset = to).collectLatest {
                (it as? DataResource.Data)?.data?.let { rate ->
                    updateState { state ->
                        state.copy(
                            outputToFiatExchangeRate = rate
                        )
                    }
                }
            }
        }
    }
}

data class ConfirmationModelState(
    val transaction: DexTransaction?,
    val inputToFiatExchangeRate: ExchangeRate?,
    val outputToFiatExchangeRate: ExchangeRate?,
    val priceUpdatedAndNotAccepted: Boolean,
    val acceptedQuoteRates: List<Money> = emptyList(),
    val networkFeesToFiatExchangeRate: ExchangeRate?,
    val operationInProgress: Boolean = false
) : ModelState

sealed class ConfirmationIntent : Intent<ConfirmationModelState> {
    object LoadTransactionData : ConfirmationIntent()
    object ConfirmSwap : ConfirmationIntent()
    object AcceptPrice : ConfirmationIntent()
    object StopListeningForUpdates : ConfirmationIntent()
    object SubscribeForTxUpdates : ConfirmationIntent()
    object UnSubscribeToTxUpdates : ConfirmationIntent()
}

sealed class ConfirmationNavigationEvent : NavigationEvent {
    object TxInProgressNavigationEvent : ConfirmationNavigationEvent()
}

sealed class ConfirmationScreenViewState : ViewState {
    object Loading : ConfirmationScreenViewState()

    data class DataConfirmationViewState(
        val inputAmount: Money?,
        val exchangeInputAmount: Money?,
        val outputAmount: Money?,
        val outputExchangeAmount: Money?,
        val inputCurrency: AssetInfo?,
        val outputCurrency: AssetInfo?,
        val inputBalance: Money?,
        val operationInProgress: Boolean,
        val newPriceAvailable: Boolean,
        val outputBalance: Money?,
        val dexExchangeRate: BigDecimal?,
        val slippage: Double?,
        val errors: List<DexUiError> = emptyList(),
        val minAmount: ConfirmationScreenExchangeAmount?,
        val networkFee: ConfirmationScreenExchangeAmount?,
        val blockchainFee: ConfirmationScreenExchangeAmount?
    ) : ConfirmationScreenViewState() {
        val alertError: AlertError?
            get() = errors.filterIsInstance<AlertError>().firstOrNull()

        val commonUiError: DexUiError.CommonUiError?
            get() = errors.filterIsInstance<DexUiError.CommonUiError>().firstOrNull()
    }
}

data class ConfirmationScreenExchangeAmount(
    val value: Money,
    val exchange: Money
)
