package com.dex.presentation.enteramount

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.extensions.safeLet
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.getOrNull
import com.blockchain.preferences.CurrencyPrefs
import com.dex.domain.AllowanceService
import com.dex.domain.AllowanceTransactionProcessor
import com.dex.domain.AllowanceTransactionState
import com.dex.domain.DexAccountsService
import com.dex.domain.DexChainService
import com.dex.domain.DexTransaction
import com.dex.domain.DexTransactionProcessor
import com.dex.domain.DexTxError
import com.dex.domain.SlippageService
import com.dex.presentation.uierrors.AlertError
import com.dex.presentation.uierrors.DexUiError
import com.dex.presentation.uierrors.toUiErrors
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import info.blockchain.balance.canConvert
import info.blockchain.balance.isLayer2Token
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
    private val enviromentConfig: EnvironmentConfig,
    private val dexAllowanceService: AllowanceService,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val dexChainService: DexChainService
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
            errors = state.transaction?.toUiErrors()?.filter { it !in state.ignoredTxErrors } ?: emptyList(),
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
            previewActionButtonState = actionButtonState(state),
            allowanceCanBeRevoked = state.canRevokeAllowance,
        )
    }

    private fun actionButtonState(modelState: AmountModelState): ActionButtonState {
        val transaction = modelState.transaction ?: return ActionButtonState.INVISIBLE
        val hasOperationInProgress =
            modelState.operationInProgress != DexOperation.NONE
        val hasValidQuote = transaction.quote != null

        if ((hasOperationInProgress || !hasValidQuote) && transaction.txErrors.isEmpty()) {
            return ActionButtonState.DISABLED
        }

        return when {
            transaction.txErrors.isEmpty() -> if (transaction.amount?.isPositive == true) {
                return ActionButtonState.ENABLED
            } else {
                ActionButtonState.DISABLED
            }
            transaction.txErrors.any {
                it is DexTxError.FatalTxError ||
                    it == DexTxError.NotEnoughFunds ||
                    it == DexTxError.NotEnoughGas ||
                    it is DexTxError.QuoteError ||
                    it == DexTxError.TokenNotAllowed
            } -> ActionButtonState.INVISIBLE
            else -> ActionButtonState.ENABLED
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
            InputAmountIntent.RevokeSourceCurrencyAllowance -> revokeSourceAllowance(modelState)
            InputAmountIntent.SubscribeForTxUpdates -> txProcessor.subscribeForTxUpdates()
            InputAmountIntent.UnSubscribeToTxUpdates -> txProcessor.unsubscribeToTxUpdates()
            InputAmountIntent.IgnoreTxInProcessError -> {
                updateState {
                    it.copy(ignoredTxErrors = it.ignoredTxErrors.plus(DexUiError.TransactionInProgressError))
                }
            }
            InputAmountIntent.AllowanceTransactionDeclined -> {
                modelState.transaction?.sourceAccount?.currency?.displayTicker?.let {
                    navigate(
                        AmountNavigationEvent.AllowanceTxFailed(
                            currencyTicker = it,
                            reason = AllowanceFailedReason.DECLINED
                        )
                    )
                }
            }
        }
    }

    private suspend fun revokeSourceAllowance(modelState: AmountModelState) {
        modelState.transaction?.sourceAccount?.currency?.let {
            updateState { state ->
                state.copy(operationInProgress = DexOperation.PUSHING_ALLOWANCE_TX)
            }
            allowanceProcessor.revokeAllowance(it)
            val result = dexAllowanceService.revokeAllowanceTransactionProgress(it)
            if (result == AllowanceTransactionState.COMPLETED) {
                updateState { state ->
                    state.copy(
                        operationInProgress = DexOperation.NONE,
                        canRevokeAllowance = false
                    )
                }
            } else {
                updateState { state ->
                    state.copy(
                        operationInProgress = DexOperation.NONE,
                    )
                }
            }
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

            val txPushOutcome = allowanceProcessor.pushTx()
            if (txPushOutcome is Outcome.Success) {
                pollForAllowance()
            } else {
                updateState {
                    it.copy(
                        operationInProgress = DexOperation.NONE,
                    )
                }
            }
        }
    }

    private suspend fun pollForAllowance() {
        val currency = modelState.transaction?.sourceAccount?.currency
        check(currency != null)
        val allowanceState = dexAllowanceService.allowanceTransactionProgress(
            assetInfo = currency
        )
        if (allowanceState == AllowanceTransactionState.COMPLETED) {
            updateState {
                it.copy(
                    operationInProgress = DexOperation.NONE,
                )
            }
            navigate(AmountNavigationEvent.AllowanceTxCompleted(currency.displayTicker))
            txProcessor.revalidate()
        } else {
            updateState {
                it.copy(
                    operationInProgress = DexOperation.NONE,
                )
            }
            navigate(
                AmountNavigationEvent.AllowanceTxFailed(
                    currencyTicker = currency.displayTicker,
                    reason = AllowanceFailedReason.TIMEOUT
                )
            )
        }
    }

    private fun initTransaction() {
        viewModelScope.launch {
            val selectedSlippage = dexSlippageService.selectedSlippage()
            // collect chain changes and reset transaction when it changes
            dexChainService.chainId.collectLatest { chainId ->
                val preselectedAccount = dexAccountsService.defSourceAccount(chainId = chainId)
                preselectedAccount?.let { source ->
                    val preselectedDestination = dexAccountsService.defDestinationAccount(
                        chainId = chainId,
                        sourceTicker = source.currency.networkTicker
                    )
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
    }

    private fun subscribeForTxUpdates() {
        viewModelScope.launch {
            txProcessor.transaction.onEach {
                updateFiatExchangeRatesIfNeeded(it)
            }.onEach {
                checkIfAllowanceCanBeRevoked(it)
            }.collectLatest {
                updateState { state ->
                    state.copy(
                        transaction = it,
                    )
                }
            }
        }

        viewModelScope.launch {
            txProcessor.quoteFetching.collectLatest { isFetchingQuote ->
                updateState { state ->
                    state.copy(
                        operationInProgress = when {
                            isFetchingQuote && state.operationInProgress == DexOperation.NONE ->
                                DexOperation.PRICE_FETCHING
                            !isFetchingQuote && state.operationInProgress == DexOperation.PRICE_FETCHING ->
                                DexOperation.NONE
                            else -> state.operationInProgress
                        }
                    )
                }
            }
        }
    }

    private suspend fun checkIfAllowanceCanBeRevoked(transaction: DexTransaction) {
        if (transaction.sourceAccount.currency.isLayer2Token && enviromentConfig.isRunningInDebugMode()) {
            val allowanceOutcome = dexAllowanceService.tokenAllowance(transaction.sourceAccount.currency)
            (allowanceOutcome as? Outcome.Success)?.value?.let { allowance ->
                if (allowance.isTokenAllowed) {
                    updateState { state ->
                        state.copy(
                            canRevokeAllowance = true
                        )
                    }
                } else {
                    updateState { state ->
                        state.copy(
                            canRevokeAllowance = false
                        )
                    }
                }
            }
        } else {
            updateState { state ->
                state.copy(
                    canRevokeAllowance = false
                )
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
        val allowanceCanBeRevoked: Boolean,
        val uiFee: UiFee?,
        val previewActionButtonState: ActionButtonState,
        private val errors: List<DexUiError> = emptyList(),
    ) : InputAmountViewState() {
        fun canChangeInputCurrency() = operationInProgress != DexOperation.PUSHING_ALLOWANCE_TX

        val topScreenUiError: DexUiError.CommonUiError?
            get() = errors.filterIsInstance<DexUiError.CommonUiError>().firstOrNull()

        val txInProgressWarning: DexUiError.TransactionInProgressError?
            get() = errors.filterIsInstance<DexUiError.TransactionInProgressError>().firstOrNull()

        val noTokenAllowanceError: DexUiError.TokenNotAllowed?
            get() = errors.filterIsInstance<DexUiError.TokenNotAllowed>().firstOrNull()

        val alertError: AlertError?
            get() = errors.filterIsInstance<AlertError>().firstOrNull()
    }

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
    val ignoredTxErrors: List<DexUiError> = emptyList(),
    val operationInProgress: DexOperation = DexOperation.NONE,
    val inputToFiatExchangeRate: ExchangeRate?,
    val outputToFiatExchangeRate: ExchangeRate?,
    val feeToFiatExchangeRate: ExchangeRate?,
    val canRevokeAllowance: Boolean = false,
) : ModelState

sealed class AmountNavigationEvent : NavigationEvent {
    data class ApproveAllowanceTx(val data: AllowanceTxUiData) : AmountNavigationEvent()
    data class AllowanceTxFailed(val currencyTicker: String, val reason: AllowanceFailedReason) :
        AmountNavigationEvent()

    data class AllowanceTxCompleted(val currencyTicker: String) :
        AmountNavigationEvent()
}

enum class AllowanceFailedReason {
    DECLINED, TIMEOUT, FAILED
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
    object AllowanceTransactionDeclined : InputAmountIntent()
    object BuildAllowanceTransaction : InputAmountIntent()
    object IgnoreTxInProcessError : InputAmountIntent()
    object RevokeSourceCurrencyAllowance : InputAmountIntent()
    class AmountUpdated(val amountString: String) : InputAmountIntent()
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
