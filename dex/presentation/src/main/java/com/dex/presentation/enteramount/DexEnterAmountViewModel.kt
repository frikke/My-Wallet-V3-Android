package com.dex.presentation.enteramount

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.dex.presentation.R
import com.blockchain.extensions.safeLet
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.getOrNull
import com.blockchain.preferences.CurrencyPrefs
import com.dex.domain.AllowanceTransactionProcessor
import com.dex.domain.DexAccountsService
import com.dex.domain.DexTransaction
import com.dex.domain.DexTransactionProcessor
import com.dex.domain.DexTxError
import com.dex.domain.SlippageService
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import info.blockchain.balance.canConvert
import info.blockchain.balance.isNetworkNativeAsset
import java.math.BigDecimal
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DexEnterAmountViewModel(
    private val currencyPrefs: CurrencyPrefs,
    private val txProcessor: DexTransactionProcessor,
    private val allowanceProcessor: AllowanceTransactionProcessor,
    private val dexAccountsService: DexAccountsService,
    private val dexSlippageService: SlippageService,
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
            maxAmount = transaction?.sourceAccount?.takeIf { !it.currency.isNetworkNativeAsset() }?.balance,
            txAmount = transaction?.amount,
            error = state.toUiError(),
            inputExchangeAmount = safeLet(transaction?.amount, state.inputToFiatExchangeRate) { amount, rate ->
                fiatAmount(amount, rate)
            } ?: Money.zero(currencyPrefs.selectedFiatCurrency),
            outputAmount = transaction?.quote?.outputAmount?.expectedOutput,
            outputExchangeAmount = safeLet(
                transaction?.quote?.outputAmount?.expectedOutput, state.outputToFiatExchangeRate
            ) { amount, rate ->
                fiatAmount(amount, rate)
            } ?: Money.zero(currencyPrefs.selectedFiatCurrency),
            destinationAccountBalance = transaction?.destinationAccount?.balance,
            sourceAccountBalance = transaction?.sourceAccount?.balance,
            operationInProgress = state.operationInProgress,
            uiFee = uiFee(
                transaction?.quote?.networkFees,
                state.feeToFiatExchangeRate
            ),
            previewActionButtonState = actionButtonState(modelState)
        )
    }

    private fun actionButtonState(modelState: AmountModelState): ActionButtonState {
        val transaction = modelState.transaction ?: return ActionButtonState.INVISIBLE
        val hasOperationInProgress = modelState.operationInProgress != DexOperation.NONE
        val hasValidQuote = transaction.quote != null
        if (hasOperationInProgress || !hasValidQuote) {
            return ActionButtonState.DISABLED
        }

        return when (transaction.txError) {
            DexTxError.None -> {
                if (transaction.amount?.isPositive == true) {
                    return ActionButtonState.ENABLED
                } else {
                    ActionButtonState.DISABLED
                }
            }
            is DexTxError.FatalTxError,
            DexTxError.NotEnoughFunds,
            DexTxError.NotEnoughGas,
            is DexTxError.QuoteError,
            DexTxError.TokenNotAllowed -> ActionButtonState.INVISIBLE
        }
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

    override fun onCleared() {
        super.onCleared()
        txProcessor.dispose()
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

            InputAmountIntent.AllowanceTransactionApproved -> {
                approveAllowanceTransaction()
            }
            InputAmountIntent.BuildAllowanceTransaction -> buildAllowanceTransaction()
            InputAmountIntent.SubscribeForTxUpdates -> txProcessor.subscribeForTxUpdates()
            InputAmountIntent.UnSubscribeToTxUpdates -> txProcessor.unsubscribeToTxUpdates()
        }
    }

    private fun buildAllowanceTransaction() {
        viewModelScope.launch {
            modelState.transaction?.sourceAccount?.currency?.let {
                updateState { modelState ->
                    modelState.copy(
                        operationInProgress = DexOperation.BUILDING_ALLOWANCE_TX
                    )
                }

                allowanceProcessor.buildTx(it).getOrNull()?.let { tx ->
                    val exchangeRate = exchangeRatesDataManager.exchangeRateToUserFiatFlow(fromAsset = tx.fees.currency)
                        .filterNot { res -> res == DataResource.Loading }.first() as? DataResource.Data
                    navigate(
                        AmountNavigationEvent.ApproveAllowanceTx(
                            AllowanceTxUiData(
                                currencyTicker = tx.currencyToAllow.networkTicker,
                                networkNativeAssetTicker = tx.fees.currency.networkTicker,
                                fiatFees = (exchangeRate?.data?.convert(tx.fees) ?: tx.fees).toStringWithSymbol()
                            )
                        )
                    )
                }

                updateState { modelState ->
                    modelState.copy(
                        operationInProgress = DexOperation.NONE
                    )
                }
            }
        }
    }

    private fun approveAllowanceTransaction() {
        viewModelScope.launch {
            updateState {
                it.copy(operationInProgress = DexOperation.PUSHING_ALLOWANCE_TX)
            }
            /**
             * todo handle failure
             */
            allowanceProcessor.pushTx().doOnSuccess {
                modelState.transaction?.sourceAccount?.currency?.let { sourceCurrency ->
                    updateState { state ->
                        state.copy(
                            currenciesToConsideredAllowed = state.currenciesToConsideredAllowed.plus(sourceCurrency)
                        )
                    }
                }
            }

            updateState {
                it.copy(
                    operationInProgress = DexOperation.NONE,
                )
            }
        }
    }

    private fun initTransaction() {
        viewModelScope.launch {
            val selectedSlippage = dexSlippageService.selectedSlippage()
            val preselectedAccount = dexAccountsService.defSourceAccount()
            preselectedAccount?.let { source ->
                val preselectedDestination = dexAccountsService.defDestinationAccount(source)
                updateState { state ->
                    state.copy(
                        canTransact = DataResource.Data(true)
                    )
                }
                txProcessor.initTransaction(
                    sourceAccount = source,
                    destinationAccount = preselectedDestination,
                    slippage = selectedSlippage
                )
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
                        transaction = it,
                    )
                }
            }
        }

        viewModelScope.launch {
            txProcessor.quoteFetching.collectLatest {
                updateState { state ->
                    state.copy(
                        operationInProgress = if (it) DexOperation.PRICE_FETCHING else DexOperation.NONE
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

        dexTransaction.quote?.networkFees?.currency?.let {
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

    private fun AmountModelState.toUiError(): DexUiError {
        val error = this.transaction?.txError ?: return DexUiError.None
        return when (error) {
            DexTxError.None -> DexUiError.None
            DexTxError.NotEnoughFunds -> DexUiError.InsufficientFunds(
                this.transaction.sourceAccount.currency
            )
            DexTxError.NotEnoughGas -> {
                val feeCurrency = this.transaction.quote?.networkFees?.currency
                check(feeCurrency != null)
                DexUiError.NotEnoughGas(
                    feeCurrency
                )
            }
            is DexTxError.FatalTxError -> DexUiError.UnknownError(error.exception)
            is DexTxError.QuoteError ->
                if (error.isLiquidityError()) DexUiError.LiquidityError
                else
                    DexUiError.CommonUiError(
                        error.title,
                        error.message
                    )

            /**
             * ignore allowance error in case token has been allowed
             */
            DexTxError.TokenNotAllowed -> if (transaction.sourceAccount.currency
                !in modelState.currenciesToConsideredAllowed
            )
                DexUiError.TokenNotAllowed(
                    transaction.sourceAccount.currency
                ) else DexUiError.None
        }
    }
}

sealed class InputAmountViewState : ViewState {
    data class TransactionInputState(
        val sourceCurrency: Currency?,
        val destinationCurrency: Currency?,
        val maxAmount: Money?,
        val txAmount: Money?,
        val operationInProgress: DexOperation,
        val destinationAccountBalance: Money?,
        val sourceAccountBalance: Money?,
        val inputExchangeAmount: Money?,
        val outputExchangeAmount: Money?,
        val outputAmount: Money?,
        val uiFee: UiFee?,
        val previewActionButtonState: ActionButtonState,
        val error: DexUiError = DexUiError.None,
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
    val operationInProgress: DexOperation = DexOperation.NONE,
    val inputToFiatExchangeRate: ExchangeRate?,
    val outputToFiatExchangeRate: ExchangeRate?,
    /**
     * when an allowance tx is getting pushed, this doesnt mean that is confirmed, so BE
     * might return allowance 0 after for some time after that tx, so we need to ignore this error so user wont
     * do the same transaction again.
     */
    val currenciesToConsideredAllowed: List<AssetInfo> = emptyList(),
    val feeToFiatExchangeRate: ExchangeRate?,
) : ModelState

sealed class AmountNavigationEvent : NavigationEvent {
    data class ApproveAllowanceTx(val data: AllowanceTxUiData) : AmountNavigationEvent()
}

sealed class InputAmountIntent : Intent<AmountModelState> {
    object InitTransaction : InputAmountIntent() {
        override fun isValidFor(modelState: AmountModelState): Boolean {
            return modelState.transaction == null ||
                modelState.transaction.hasBeenExecuted()
        }
    }

    object SubscribeForTxUpdates : InputAmountIntent()
    object UnSubscribeToTxUpdates : InputAmountIntent()
    object AllowanceTransactionApproved : InputAmountIntent()
    object BuildAllowanceTransaction : InputAmountIntent()
    class AmountUpdated(val amountString: String) : InputAmountIntent()
}

sealed class DexUiError {
    object None : DexUiError()
    data class InsufficientFunds(val currency: Currency) : DexUiError(), AlertError {
        override fun message(context: Context): String =
            context.getString(R.string.not_enough_funds, currency.displayTicker)
    }

    object LiquidityError : DexUiError(), AlertError {
        override fun message(context: Context): String =
            context.getString(R.string.unable_to_swap_tokens)
    }

    data class TokenNotAllowed(val token: Currency) : DexUiError()
    data class NotEnoughGas(val gasCurrency: Currency) : DexUiError(), AlertError {
        override fun message(context: Context): String =
            context.getString(R.string.not_enough_gas, gasCurrency.displayTicker)
    }

    data class CommonUiError(val title: String, val description: String) : DexUiError()
    data class UnknownError(val exception: Exception) : DexUiError()
}

interface AlertError {
    fun message(context: Context): String
}

enum class DexOperation {
    NONE, PRICE_FETCHING, PUSHING_ALLOWANCE_TX, BUILDING_ALLOWANCE_TX
}

@kotlinx.serialization.Serializable
data class AllowanceTxUiData(
    val currencyTicker: String,
    val networkNativeAssetTicker: String,
    val fiatFees: String
)

enum class ActionButtonState {
    INVISIBLE, ENABLED, DISABLED
}
