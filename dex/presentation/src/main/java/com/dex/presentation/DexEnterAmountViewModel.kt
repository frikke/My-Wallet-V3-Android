package com.dex.presentation

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.preferences.CurrencyPrefs
import com.dex.domain.DexAccountsService
import com.dex.domain.DexTransaction
import com.dex.domain.DexTransactionProcessor
import com.dex.domain.EmptyDexTransaction
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import info.blockchain.balance.canConvert
import java.math.BigDecimal
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DexEnterAmountViewModel(
    private val currencyPrefs: CurrencyPrefs,
    private val txProcessor: DexTransactionProcessor,
    private val dexAccountsService: DexAccountsService,
    private val exchangeRatesDataManager: ExchangeRatesDataManager
) : MviViewModel<
    InputAmountIntent,
    InputAmountViewState,
    AmountModelState,
    AmountNavigationEvent,
    ModelConfigArgs.NoArgs
    >(
    initialState = AmountModelState(
        transaction = EmptyDexTransaction,
        inputToFiatExchangeRate = null
    )
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun reduce(state: AmountModelState): InputAmountViewState {
        with(state) {
            return InputAmountViewState(
                sourceCurrency = transaction.sourceAccount?.currency,
                destinationCurrency = transaction.destinationAccount?.currency,
                maxAmount = transaction.sourceAccount?.balance,
                inputExchangeAmount = fiatInputAmount(transaction.amount, inputToFiatExchangeRate),
                outputExchangeAmountIntent = Money.zero(currencyPrefs.selectedFiatCurrency),
                destinationAccountBalance = transaction.destinationAccount?.balance
            )
        }
    }

    private fun fiatInputAmount(amount: Money?, inputToFiatExchangeRate: ExchangeRate?): Money {
        val inputAmount = amount ?: return Money.zero(currencyPrefs.selectedFiatCurrency)
        val exchangeRate = inputToFiatExchangeRate ?: return Money.zero(currencyPrefs.selectedFiatCurrency)
        return exchangeRate.takeIf { it.canConvert(inputAmount) }?.let {
            it.convert(inputAmount)
        } ?: return Money.zero(currencyPrefs.selectedFiatCurrency)
    }

    override suspend fun handleIntent(modelState: AmountModelState, intent: InputAmountIntent) {
        when (intent) {
            InputAmountIntent.InitTransaction -> {
                txProcessor.initTransaction()
                subscribeForTxUpdates()
                preselectSourceAccount()
            }
            is InputAmountIntent.AmountUpdated ->
                when {
                    intent.amountString.isEmpty() -> txProcessor.updateTransactionAmount(BigDecimal.ZERO)
                    intent.amountString.toDoubleOrNull() != null -> txProcessor.updateTransactionAmount(
                        intent.amountString.toBigDecimal()
                    )
                    else -> {
                        // invalid amount todo
                    }
                }
        }
    }

    private fun preselectSourceAccount() {
        viewModelScope.launch {
            dexAccountsService.defSourceAccount().collect {
                txProcessor.updateSourceAccount(it)
            }
        }
    }

    private fun subscribeForTxUpdates() {
        viewModelScope.launch {
            txProcessor.transaction.onEach {
                val sourceCurrency = it.sourceAccount?.currency ?: return@onEach
                if (modelState.inputToFiatExchangeRate?.from != sourceCurrency) {
                    updateInputFiatExchangeRate(
                        from = sourceCurrency,
                        to = currencyPrefs.selectedFiatCurrency
                    )
                }
            }.collectLatest {
                updateState { state ->
                    state.copy(
                        transaction = it
                    )
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
}

data class InputAmountViewState(
    val sourceCurrency: Currency?,
    val destinationCurrency: Currency?,
    val maxAmount: Money?,
    val destinationAccountBalance: Money?,
    val inputExchangeAmount: Money?,
    val outputExchangeAmountIntent: Money?
) : ViewState

data class AmountModelState(
    val transaction: DexTransaction,
    val inputToFiatExchangeRate: ExchangeRate?,
) : ModelState

class AmountNavigationEvent : NavigationEvent

sealed class InputAmountIntent : Intent<AmountModelState> {
    object InitTransaction : InputAmountIntent()
    class AmountUpdated(val amountString: String) : InputAmountIntent()
}
