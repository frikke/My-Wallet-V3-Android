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
import com.blockchain.extensions.safeLet
import com.blockchain.preferences.CurrencyPrefs
import com.dex.domain.DexAccountsService
import com.dex.domain.DexTransaction
import com.dex.domain.DexTransactionProcessor
import com.dex.domain.DexTxError
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import info.blockchain.balance.canConvert
import java.math.BigDecimal
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
        transaction = null,
        inputToFiatExchangeRate = null,
        outputToFiatExchangeRate = null,
        feeToFiatExchangeRate = null,
        canTransact = DataResource.Loading
    )
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun reduce(state: AmountModelState): InputAmountViewState {
        with(state) {
            return when (canTransact) {
                DataResource.Loading -> InputAmountViewState.Loading
                is DataResource.Data -> if (canTransact.data) {
                    showInputUi(state)
                } else {
                    showNoInoutUi()
                }
                /**
                 * todo map loading errors
                 * */
                else -> throw IllegalStateException("Model state cannot be mapped")
            }
        }
    }

    private fun showNoInoutUi(): InputAmountViewState =
        InputAmountViewState.NoInputViewState

    private fun showInputUi(state: AmountModelState): InputAmountViewState {
        val transaction = state.transaction
        return InputAmountViewState.TransactionInputState(
            sourceCurrency = transaction?.sourceAccount?.currency,
            destinationCurrency = transaction?.destinationAccount?.currency,
            maxAmount = transaction?.sourceAccount?.balance,
            error = transaction?.toUiError() ?: DexUiError.None,
            inputExchangeAmount = safeLet(transaction?.amount, state.inputToFiatExchangeRate) { amount, rate ->
                fiatAmount(amount, rate)
            } ?: Money.zero(currencyPrefs.selectedFiatCurrency),
            outputAmount = transaction?.outputAmount?.expectedOutput,
            outputExchangeAmount = safeLet(
                transaction?.outputAmount?.expectedOutput, state.outputToFiatExchangeRate
            ) { amount, rate ->
                fiatAmount(amount, rate)
            } ?: Money.zero(currencyPrefs.selectedFiatCurrency),
            destinationAccountBalance = transaction?.destinationAccount?.balance,
            uiFee = uiFee(
                transaction?.fees,
                state.feeToFiatExchangeRate
            )
        )
    }

    private fun uiFee(fees: Money?, feeToFiatExchangeRate: ExchangeRate?): UiFee? {
        return fees?.let { fee ->
            UiFee(
                fee = fee,
                feeInFiat = feeToFiatExchangeRate?.let {
                    fiatAmount(fee, it)
                }
            )
        }
    }

    private fun fiatAmount(amount: Money, exchangeRate: ExchangeRate): Money {
        return exchangeRate.takeIf { it.canConvert(amount) }?.convert(amount) ?: return Money.zero(
            currencyPrefs.selectedFiatCurrency
        )
    }

    override suspend fun handleIntent(modelState: AmountModelState, intent: InputAmountIntent) {
        when (intent) {
            InputAmountIntent.InitTransaction -> {
                initTransaction()
            }
            is InputAmountIntent.AmountUpdated -> {
                val amount = when {
                    intent.amountString.isEmpty() -> BigDecimal.ZERO
                    intent.amountString.toBigDecimalOrNull() != null -> intent.amountString.toBigDecimal()
                    else -> null
                }
                amount?.let {
                    txProcessor.updateTransactionAmount(amount)
                }
            }
            InputAmountIntent.DisposeTransaction -> {
            }
        }
    }

    private fun initTransaction() {
        viewModelScope.launch {
            val preselectedAccount = dexAccountsService.defSourceAccount()
            preselectedAccount?.let {
                updateState { state ->
                    state.copy(
                        canTransact = DataResource.Data(true)
                    )
                }
                txProcessor.initTransaction(it)
                subscribeForTxUpdates()
            } ?: updateState { state ->
                state.copy(
                    canTransact = DataResource.Data(false)
                )
            }
        }
    }

    private fun subscribeForTxUpdates() {
        viewModelScope.launch {
            txProcessor.transaction.onEach {
                updateFiatExchangeRatesIfNeeded(it)
            }.collectLatest {
                updateState { state ->
                    state.copy(
                        transaction = it
                    )
                }
            }
        }
    }

    private fun updateFiatExchangeRatesIfNeeded(dexTransaction: DexTransaction) {
        if (modelState.inputToFiatExchangeRate?.from != dexTransaction.sourceAccount.currency) {
            updateInputFiatExchangeRate(
                from = dexTransaction.sourceAccount.currency,
                to = currencyPrefs.selectedFiatCurrency
            )
        }

        dexTransaction.fees?.currency?.let {
            if (modelState.feeToFiatExchangeRate?.from != it) {
                updateFeeFiatExchangeRate(
                    from = it,
                    to = currencyPrefs.selectedFiatCurrency
                )
            }
        }

        dexTransaction.destinationAccount?.currency?.let {
            if (modelState.outputToFiatExchangeRate?.from != it) {
                updateOutputFiatExchangeRate(
                    from = it,
                    to = currencyPrefs.selectedFiatCurrency
                )
            }
        }
    }

    private fun updateFeeFiatExchangeRate(from: Currency, to: FiatCurrency) {
        viewModelScope.launch {
            exchangeRatesDataManager.exchangeRate(fromAsset = from, toAsset = to).collectLatest {
                (it as? DataResource.Data)?.data?.let { rate ->
                    updateState { state ->
                        state.copy(
                            feeToFiatExchangeRate = rate
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

    private fun DexTransaction.toUiError() =
        when (this.txError) {
            DexTxError.None -> DexUiError.None
            DexTxError.NotEnoughFunds -> DexUiError.InsufficientFunds(
                sourceAccount.currency
            )
            DexTxError.NotEnoughGas -> {
                val feeCurrency = fees?.currency
                check(feeCurrency != null)
                DexUiError.NotEnoughGas(
                    feeCurrency
                )
            }
        }
}

sealed class InputAmountViewState : ViewState {
    data class TransactionInputState(
        val sourceCurrency: Currency?,
        val destinationCurrency: Currency?,
        val maxAmount: Money?,
        val destinationAccountBalance: Money?,
        val inputExchangeAmount: Money?,
        val outputExchangeAmount: Money?,
        val outputAmount: Money?,
        val uiFee: UiFee?,
        val error: DexUiError = DexUiError.None
    ) : InputAmountViewState()

    object NoInputViewState : InputAmountViewState()
    object Loading : InputAmountViewState()
}

data class UiFee(
    val feeInFiat: Money?,
    val fee: Money
)

data class AmountModelState(
    val canTransact: DataResource<Boolean> = DataResource.Loading,
    val transaction: DexTransaction?,
    val inputToFiatExchangeRate: ExchangeRate?,
    val outputToFiatExchangeRate: ExchangeRate?,
    val feeToFiatExchangeRate: ExchangeRate?,
) : ModelState

class AmountNavigationEvent : NavigationEvent

sealed class InputAmountIntent : Intent<AmountModelState> {
    object InitTransaction : InputAmountIntent()
    object DisposeTransaction : InputAmountIntent()
    class AmountUpdated(val amountString: String) : InputAmountIntent()
}

sealed class DexUiError {
    object None : DexUiError()
    data class InsufficientFunds(val currency: Currency) : DexUiError()
    data class NotEnoughGas(val gasCurrency: Currency) : DexUiError()
}
