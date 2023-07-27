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
import com.dex.domain.ExchangeAmount
import com.dex.presentation.uierrors.AlertError
import com.dex.presentation.uierrors.DexUiError
import com.dex.presentation.uierrors.uiErrors
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

    override fun ConfirmationModelState.reduce(): ConfirmationScreenViewState {
        val transaction = transaction ?: return ConfirmationScreenViewState.Loading
        val sellAmount = (transaction.inputAmount as? ExchangeAmount.SellAmount)?.amount
            ?: transaction.quote?.sellAmount?.amount
        val buyAmount = (transaction.inputAmount as? ExchangeAmount.BuyAmount)?.amount
            ?: transaction.quote?.buyAmount?.amount
        val minAmount = transaction.quote?.buyAmount?.minAmount
        val minAmountExchangeRate = when {
            transaction.quote?.buyAmount?.minAmount != null -> buyAmountToFiatExchangeRate
            transaction.quote?.sellAmount?.minAmount != null -> sellAmountToFiatExchangeRate
            else -> throw IllegalStateException("Unknown min amount exchange rate")
        }
        return ConfirmationScreenViewState.DataConfirmationViewState(
            sellAmount = sellAmount,
            exchangeSellAmount = safeLet(sellAmount, sellAmountToFiatExchangeRate) { amount, rate ->
                rate.convert(amount)
            } ?: Money.zero(currencyPrefs.selectedFiatCurrency),
            buyAmount = buyAmount,
            buyExchangeAmount = safeLet(
                buyAmount,
                buyAmountToFiatExchangeRate
            ) { amount, rate ->
                rate.convert(amount)
            },
            sellCurrency = transaction.sourceAccount.currency,
            buyCurrency = transaction.destinationAccount?.currency,
            sellAccountBalance = transaction.sourceAccount.balance,
            operationInProgress = operationInProgress,
            buyAccountBalance = transaction.destinationAccount?.balance,
            dexExchangeRate = safeLet(buyAmount, sellAmount) { bAmount, sAmount ->
                bAmount.toBigDecimal().divide(sAmount.toBigDecimal(), 2, RoundingMode.HALF_UP).max(
                    BigDecimal("0.001")
                )
            },
            slippage = transaction.slippage,
            minAmount = safeLet(
                minAmount,
                minAmountExchangeRate
            ) { amount, exchangeRate ->
                ConfirmationScreenExchangeAmount(
                    value = amount,
                    exchange = exchangeRate.convert(amount)
                )
            },
            networkFee = safeLet(
                transaction.quote?.networkFees,
                networkFeesToFiatExchangeRate
            ) { amount, exchangeRate ->
                ConfirmationScreenExchangeAmount(
                    value = amount,
                    exchange = exchangeRate.convert(amount)
                )
            },
            blockchainFee = safeLet(
                transaction.quote?.blockchainFees,
                buyAmountToFiatExchangeRate
            ) { amount, exchangeRate ->
                ConfirmationScreenExchangeAmount(
                    value = amount,
                    exchange = exchangeRate.convert(amount)
                )
            },
            /*
             * Ignore this error in the confirmation screen
             * */
            errors = transaction.uiErrors().filter { it !is DexUiError.TransactionInProgressError },
            newPriceAvailable = priceUpdatedAndNotAccepted,
            network = transaction.sourceAccount.currency.coinNetwork?.shortName
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
                updateState {
                    copy(
                        priceUpdatedAndNotAccepted = false,
                        acceptedQuoteRates = transaction?.quote?.price?.let {
                            acceptedQuoteRates.plus(it)
                        } ?: acceptedQuoteRates
                    )
                }
            }
        }
    }

    private suspend fun executeTx() {
        updateState {
            copy(
                operationInProgress = true
            )
        }
        transactionProcessor.execute()
        updateState {
            copy(
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
                updateState {
                    val priceAccepted = tx.quote?.let {
                        priceHasBeenAccepted(it, acceptedQuoteRates)
                    } ?: false

                    copy(
                        transaction = tx,
                        priceUpdatedAndNotAccepted = !priceAccepted && tx.quote != null,
                        acceptedQuoteRates = if (priceAccepted) {
                            tx.quote?.price?.let {
                                acceptedQuoteRates.plus(it)
                            } ?: acceptedQuoteRates
                        } else acceptedQuoteRates
                    )
                }
            }
        }

        viewModelScope.launch {
            transactionProcessor.quoteFetching.collectLatest {
                updateState {
                    copy(
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
                    updateState {
                        copy(
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
                    updateState {
                        copy(
                            sellAmountToFiatExchangeRate = rate
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
                    updateState {
                        copy(
                            buyAmountToFiatExchangeRate = rate
                        )
                    }
                }
            }
        }
    }
}

data class ConfirmationModelState(
    val transaction: DexTransaction?,
    val sellAmountToFiatExchangeRate: ExchangeRate?,
    val buyAmountToFiatExchangeRate: ExchangeRate?,
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
        val sellAmount: Money?,
        val exchangeSellAmount: Money?,
        val buyAmount: Money?,
        val buyExchangeAmount: Money?,
        val sellCurrency: AssetInfo?,
        val buyCurrency: AssetInfo?,
        val sellAccountBalance: Money?,
        val operationInProgress: Boolean,
        val newPriceAvailable: Boolean,
        val buyAccountBalance: Money?,
        val dexExchangeRate: BigDecimal?,
        val slippage: Double?,
        val network: String?,
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
