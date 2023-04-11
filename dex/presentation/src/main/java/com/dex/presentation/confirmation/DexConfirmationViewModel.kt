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
import com.dex.domain.DexTransaction
import com.dex.domain.DexTransactionProcessor
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DexConfirmationViewModel(
    private val transactionProcessor: DexTransactionProcessor,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val currencyPrefs: CurrencyPrefs,
) : MviViewModel<
    ConfirmationIntent,
    ConfirmationScreenViewState,
    ConfirmationModelState,
    ConfirmationNavigationEvent,
    ModelConfigArgs.NoArgs
    >(initialState = ConfirmationModelState(null, null, null, null)) {
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
                transaction.quote?.outputAmount?.expectedOutput, state.outputToFiatExchangeRate
            ) { amount, rate ->
                rate.convert(amount)
            } ?: Money.zero(currencyPrefs.selectedFiatCurrency),
            inputCurrency = transaction.sourceAccount.currency,
            outputCurrency = transaction.destinationAccount?.currency,
            inputBalance = transaction.sourceAccount.balance,
            outputBalance = transaction.destinationAccount?.balance,
            dexExchangeRate = transaction.quote?.price?.toBigDecimal()?.setScale(2, RoundingMode.HALF_UP),
            slippage = transaction.slippage,
            minAmount = safeLet(
                transaction.quote?.outputAmount?.minOutputAmount, state.outputToFiatExchangeRate
            ) { amount, exchangeRate ->
                ConfirmationScreenExchangeAmount(
                    value = amount,
                    exchange = exchangeRate.convert(amount),
                )
            },
            networkFee = safeLet(
                transaction.quote?.networkFees, state.networkFeesToFiatExchangeRate
            ) { amount, exchangeRate ->
                ConfirmationScreenExchangeAmount(
                    value = amount,
                    exchange = exchangeRate.convert(amount),
                )
            },
            blockchainFee = safeLet(
                transaction.quote?.blockchainFees, state.outputToFiatExchangeRate
            ) { amount, exchangeRate ->
                ConfirmationScreenExchangeAmount(
                    value = amount,
                    exchange = exchangeRate.convert(amount),
                )
            },
        )
    }

    override suspend fun handleIntent(modelState: ConfirmationModelState, intent: ConfirmationIntent) {
        when (intent) {
            ConfirmationIntent.LoadTransactionData -> loadTransactionData()
            ConfirmationIntent.SubscribeForTxUpdates -> transactionProcessor.subscribeForTxUpdates()
            ConfirmationIntent.UnSubscribeToTxUpdates -> transactionProcessor.unsubscribeToTxUpdates()
        }
    }

    private fun loadTransactionData() {
        viewModelScope.launch {
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
                    it.copy(
                        transaction = tx
                    )
                }
            }
        }
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
    val networkFeesToFiatExchangeRate: ExchangeRate?,
) : ModelState

sealed class ConfirmationIntent : Intent<ConfirmationModelState> {
    object LoadTransactionData : ConfirmationIntent()

    object SubscribeForTxUpdates : ConfirmationIntent()
    object UnSubscribeToTxUpdates : ConfirmationIntent()
}

sealed class ConfirmationNavigationEvent : NavigationEvent

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
        val outputBalance: Money?,
        val dexExchangeRate: BigDecimal?,
        val slippage: Double?,
        val minAmount: ConfirmationScreenExchangeAmount?,
        val networkFee: ConfirmationScreenExchangeAmount?,
        val blockchainFee: ConfirmationScreenExchangeAmount?,
    ) : ConfirmationScreenViewState()
}

data class ConfirmationScreenExchangeAmount(
    val value: Money,
    val exchange: Money
)
