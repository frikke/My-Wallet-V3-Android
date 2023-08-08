package com.dex.presentation.enteramount

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.OneTimeAccountPersistenceService
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.dataOrElse
import com.blockchain.data.doOnData
import com.blockchain.data.doOnError
import com.blockchain.data.filterNotLoading
import com.blockchain.data.map
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.extensions.safeLet
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.FeatureAccess
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.getOrNull
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.utils.toBigDecimalOrNullFromLocalisedInput
import com.dex.domain.AllowanceService
import com.dex.domain.AllowanceTransactionProcessor
import com.dex.domain.AllowanceTransactionState
import com.dex.domain.DexAccountsService
import com.dex.domain.DexEligibilityService
import com.dex.domain.DexNetworkService
import com.dex.domain.DexTransaction
import com.dex.domain.DexTransactionProcessor
import com.dex.domain.DexTxError
import com.dex.domain.ExchangeAmount
import com.dex.domain.SlippageService
import com.dex.presentation.network.DexNetworkViewState
import com.dex.presentation.uierrors.ActionRequiredError
import com.dex.presentation.uierrors.AlertError
import com.dex.presentation.uierrors.DexUiError
import com.dex.presentation.uierrors.uiErrors
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CoinNetwork
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import info.blockchain.balance.canConvert
import info.blockchain.balance.isLayer2Token
import info.blockchain.balance.isNetworkNativeAsset
import java.math.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DexEnterAmountViewModel(
    private val currencyPrefs: CurrencyPrefs,
    private val txProcessor: DexTransactionProcessor,
    private val allowanceProcessor: AllowanceTransactionProcessor,
    private val dexAccountsService: DexAccountsService,
    private val dexEligibilityService: DexEligibilityService,
    private val dexSlippageService: SlippageService,
    private val enviromentConfig: EnvironmentConfig,
    private val dexAllowanceService: AllowanceService,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val dexNetworkService: DexNetworkService,
    private val assetCatalogue: AssetCatalogue,
    private val oneTimeAccountPersistenceService: OneTimeAccountPersistenceService
) : MviViewModel<
    InputAmountIntent,
    InputAmountViewState,
    AmountModelState,
    AmountNavigationEvent,
    ModelConfigArgs.NoArgs
    >(
    initialState = AmountModelState(
        transaction = null,
        sellCurrencyToFiatExchangeRate = null,
        buyCurrencyToFiatExchangeRate = null,
        feeToFiatExchangeRate = null,
        dexAllowanceState = DataResource.Loading
    )
) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun AmountModelState.reduce() = when (dexAllowanceState) {
        DataResource.Loading -> InputAmountViewState.Loading
        is DataResource.Data -> when (val allowance = dexAllowanceState.data) {
            DexAllowanceState.Allowed -> {
                showInputUi(this)
            }

            DexAllowanceState.NoFundsAvailable -> {
                showNoInoutUi(this)
            }

            is DexAllowanceState.NotEligible -> InputAmountViewState.NotEligible(allowance.reason)
        }
        /**
         * todo map loading errors
         * */
        else -> throw IllegalStateException("Model state cannot be mapped")
    }

    private fun AmountModelState.reduceSelectedNetwork(): DataResource<DexNetworkViewState> {
        val selectedNetwork = networks.map { coinNetworks ->
            coinNetworks.firstOrNull { it.chainId == selectedChain }
        }
        return (selectedNetwork as? DataResource.Data)?.data?.let {
            val assetInfo = assetCatalogue.assetInfoFromNetworkTicker(it.nativeAssetTicker)
            check(assetInfo != null)
            DataResource.Data(
                DexNetworkViewState(
                    chainId = it.chainId!!,
                    logo = assetInfo.logo,
                    name = it.shortName,
                    selected = true
                )
            )
        } ?: DataResource.Loading
    }

    private fun AmountModelState.reduceAllowNetworkSelection(): Boolean {
        return networks.map { it.size > 1 }.dataOrElse(false)
    }

    private fun showNoInoutUi(state: AmountModelState): InputAmountViewState =
        InputAmountViewState.NoInputViewState(
            selectedNetwork = state.reduceSelectedNetwork(),
            allowNetworkSelection = state.reduceAllowNetworkSelection()
        )

    private fun showInputUi(state: AmountModelState): InputAmountViewState {
        val transaction = state.transaction
        val buyAmount = (transaction?.inputAmount as? ExchangeAmount.BuyAmount)?.amount
            ?: transaction?.quote?.buyAmount?.amount
        val sellAmount = (transaction?.inputAmount as? ExchangeAmount.SellAmount)?.amount
            ?: transaction?.quote?.sellAmount?.amount
        return InputAmountViewState.TransactionInputState(
            selectedNetwork = state.reduceSelectedNetwork(),
            allowNetworkSelection = state.reduceAllowNetworkSelection(),
            sourceCurrency = transaction?.sourceAccount?.currency,
            destinationCurrency = transaction?.destinationAccount?.currency,
            maxSourceAmount = transaction?.sourceAccount?.takeIf { !it.currency.isNetworkNativeAsset() }?.balance,
            sellAmount = sellAmount,
            errors = state.transaction?.uiErrors()?.filter { it !in state.ignoredTxErrors }
                ?.withOnlyTopPriorityActionRequired() ?: emptyList(),
            sellExchangeAmount = safeLet(sellAmount, state.sellCurrencyToFiatExchangeRate) { amount, rate ->
                fiatAmount(amount, rate)
            } ?: Money.zero(currencyPrefs.selectedFiatCurrency),
            buyAmount = buyAmount,
            buyExchangeAmount = safeLet(buyAmount, state.buyCurrencyToFiatExchangeRate) { amount, rate ->
                fiatAmount(amount, rate)
            } ?: Money.zero(currencyPrefs.selectedFiatCurrency),
            destinationAccountBalance = transaction?.destinationAccount?.balance,
            sourceAccountBalance = transaction?.sourceAccount?.balance,
            operationInProgress = state.operationInProgress,
            uiFee = uiNetworkFee(
                transaction?.quote?.networkFees,
                state.feeToFiatExchangeRate
            ),
            previewActionButtonState = actionButtonState(state),
            sourceAccountHasNoFunds = transaction?.sourceAccount?.balance?.isZero ?: false,
            allowanceCanBeRevoked = state.canRevokeAllowance
        )
    }

    private fun List<DexUiError>.withOnlyTopPriorityActionRequired(): List<DexUiError> {
        val nonActionRequiredErrors = filter { it !is ActionRequiredError }
        val topPriorityActionError = filterIsInstance<ActionRequiredError>().minByOrNull { it.priority }
        return (topPriorityActionError as? DexUiError)?.let {
            nonActionRequiredErrors + it
        } ?: nonActionRequiredErrors
    }

    private fun actionButtonState(modelState: AmountModelState): ActionButtonState {
        val transaction = modelState.transaction ?: return ActionButtonState.INVISIBLE
        val hasOperationInProgress =
            modelState.operationInProgress != DexOperation.None
        val hasValidQuote = transaction.quote != null

        if ((hasOperationInProgress || !hasValidQuote) && transaction.txErrors.isEmpty()) {
            return ActionButtonState.DISABLED
        }

        return when {
            transaction.txErrors.isEmpty() -> if (transaction.inputAmount?.isPositive == true) {
                return ActionButtonState.ENABLED
            } else {
                ActionButtonState.DISABLED
            }

            transaction.txErrors.any {
                it is DexTxError.FatalTxError ||
                    it == DexTxError.NotEnoughFunds ||
                    it == DexTxError.NotEnoughGas ||
                    it is DexTxError.QuoteError ||
                    it is DexTxError.TokenNotAllowed
            } -> ActionButtonState.INVISIBLE

            else -> ActionButtonState.ENABLED
        }
    }

    private fun uiNetworkFee(fees: Money?, feeToFiatExchangeRate: ExchangeRate?): UiNetworkFee {
        return fees?.let { fee ->
            UiNetworkFee.DefinedFee(
                fee = fee,
                feeInFiat = feeToFiatExchangeRate?.let {
                    fiatAmount(fee, it)
                }
            )
        } ?: UiNetworkFee.PlaceholderFee(
            Money.zero(currencyPrefs.selectedFiatCurrency)
        )
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
                tryToInitTransaction()
            }

            is InputAmountIntent.InputAmountUpdated -> {
                val amount = when {
                    intent.amountString.isEmpty() -> BigDecimal.ZERO
                    else -> intent.amountString.toBigDecimalOrNullFromLocalisedInput()
                }
                amount?.let {
                    txProcessor.updateSellAmount(amount)
                }
            }

            InputAmountIntent.AllowanceTransactionApproved -> {
                approveAllowanceTransaction()
            }

            InputAmountIntent.BuildAllowanceTransaction -> buildAllowanceTransaction()
            InputAmountIntent.RevokeSourceCurrencyAllowance -> revokeSourceAllowance(modelState)
            InputAmountIntent.SubscribeForTxUpdates -> txProcessor.subscribeForTxUpdates()
            InputAmountIntent.UnSubscribeToTxUpdates -> txProcessor.unsubscribeToTxUpdates()
            is InputAmountIntent.ReceiveCurrencyRequested -> {
                (intent.currency as? AssetInfo)?.coinNetwork?.let {
                    dexAccountsService.nativeNetworkAccount(it).doOnSuccess { account ->
                        oneTimeAccountPersistenceService.saveAccount(account)
                        navigate(AmountNavigationEvent.ReceiveOnAccount)
                    }
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

            is InputAmountIntent.PollForPendingAllowance -> {
                pollForAllowance(intent.currency, null)
            }

            is InputAmountIntent.ViewAllowanceTx -> navigate(
                AmountNavigationEvent.AllowanceTxUrl(
                    intent.currency.coinNetwork?.explorerUrl?.plus("/${intent.txId}").orEmpty()
                )
            )

            is InputAmountIntent.DepositOnSourceAccountRequested -> {
                modelState.transaction?.sourceAccount?.let {
                    oneTimeAccountPersistenceService.saveAccount(it.account)
                    navigate(AmountNavigationEvent.ReceiveOnAccount)
                }
            }

            is InputAmountIntent.DepositOnAccountRequested -> {
                oneTimeAccountPersistenceService.saveAccount(intent.account)
                navigate(AmountNavigationEvent.ReceiveOnAccount)
            }

            InputAmountIntent.RevalidateTransaction -> viewModelScope.launch {
                txProcessor.revalidate()
            }

            is InputAmountIntent.OutputAmountUpdated -> {
                val amount = when {
                    intent.amountString.isEmpty() -> BigDecimal.ZERO
                    intent.amountString.toBigDecimalOrNull() != null -> intent.amountString.toBigDecimal()
                    else -> null
                }
                amount?.let {
                    txProcessor.updateBuyAmount(amount)
                }
            }
        }
    }

    private suspend fun revokeSourceAllowance(modelState: AmountModelState) {
        modelState.transaction?.sourceAccount?.currency?.let {
            updateState {
                copy(operationInProgress = DexOperation.PushingAllowance)
            }
            allowanceProcessor.revokeAllowance(it)
            val result = dexAllowanceService.revokeAllowanceTransactionProgress(it)
            if (result == AllowanceTransactionState.COMPLETED) {
                updateState {
                    copy(
                        operationInProgress = DexOperation.None,
                        canRevokeAllowance = false
                    )
                }
            } else {
                updateState {
                    copy(
                        operationInProgress = DexOperation.None
                    )
                }
            }
        }
    }

    private fun buildAllowanceTransaction() {
        viewModelScope.launch {
            modelState.transaction?.sourceAccount?.currency?.let {
                updateState {
                    copy(
                        operationInProgress = DexOperation.BuildingAllowance
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
                                feesCrypto = tx.fees.toStringWithSymbol(),
                                feesFiat = (exchangeRate?.data?.convert(tx.fees) ?: tx.fees).toStringWithSymbol()
                            )
                        )
                    )
                }

                updateState {
                    copy(
                        operationInProgress = DexOperation.None
                    )
                }
            }
        }
    }

    private fun approveAllowanceTransaction() {
        viewModelScope.launch {
            updateState {
                copy(operationInProgress = DexOperation.PushingAllowance)
            }

            val txPushOutcome = allowanceProcessor.pushTx()
            if (txPushOutcome is Outcome.Success) {
                pollForAllowance(
                    txPushOutcome.value.first,
                    txPushOutcome.value.second
                )
            } else {
                updateState {
                    copy(
                        operationInProgress = DexOperation.None
                    )
                }
            }
        }
    }

    private suspend fun pollForAllowance(asset: AssetInfo, txId: String?) {
        val currency = modelState.transaction?.sourceAccount?.currency
        check(currency != null)
        updateState {
            copy(operationInProgress = DexOperation.PollingAllowance(asset, txId))
        }
        val allowanceState = dexAllowanceService.allowanceTransactionProgress(
            assetInfo = currency
        )
        if (allowanceState == AllowanceTransactionState.COMPLETED) {
            updateState {
                copy(
                    operationInProgress = DexOperation.None
                )
            }
            navigate(AmountNavigationEvent.AllowanceTxCompleted(currency.displayTicker))
            txProcessor.revalidate()
        } else {
            updateState {
                copy(
                    operationInProgress = DexOperation.None
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

    private fun tryToInitTransaction() {
        viewModelScope.launch {
            dexEligibilityService.dexEligibility().filterNotLoading().doOnData { featureAccess ->
                when (featureAccess) {
                    is FeatureAccess.Granted -> initTransaction()
                    is FeatureAccess.Blocked -> updateState {
                        copy(
                            dexAllowanceState = DataResource.Data(
                                DexAllowanceState.NotEligible(
                                    (featureAccess.reason as? BlockedReason.NotEligible)?.message
                                )
                            )
                        )
                    }
                }
            }.doOnError {
                updateState {
                    copy(
                        dexAllowanceState = DataResource.Data(
                            DexAllowanceState.NotEligible(null)
                        )
                    )
                }
            }.collect()
        }
    }

    private suspend fun initTransaction() {
        val selectedSlippage = dexSlippageService.selectedSlippage()
        val networks = dexNetworkService.supportedNetworks()

        val defAccounts = networks.associateWith {
            dexAccountsService.defSourceAccount(it)
        }

        updateState {
            copy(
                networks = DataResource.Data(networks)
            )
        }

        if (defAccounts.filter { it.value.fiatBalance?.isPositive == true }.isEmpty()) {
            dexNetworkService.chainId.collectLatest { chainId ->
                updateState {
                    copy(
                        selectedChain = chainId,
                        dexAllowanceState = DataResource.Data(DexAllowanceState.NoFundsAvailable)
                    )
                }
            }
        } else {
            dexNetworkService.chainId.collectLatest { chainId ->
                val preselectedAccount = defAccounts[networks.first { it.chainId == chainId }]
                preselectedAccount?.let { source ->
                    val preselectedDestination = dexAccountsService.defDestinationAccount(
                        chainId = chainId,
                        source = source
                    )
                    updateState {
                        copy(
                            selectedChain = chainId,
                            dexAllowanceState = DataResource.Data(DexAllowanceState.Allowed)
                        )
                    }
                    txProcessor.initTransaction(
                        sourceAccount = source,
                        destinationAccount = preselectedDestination,
                        slippage = selectedSlippage
                    )
                    subscribeForTxUpdates()
                } ?: updateState {
                    copy(
                        selectedChain = chainId,
                        dexAllowanceState = DataResource.Data(DexAllowanceState.NoFundsAvailable)
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun subscribeForTxUpdates() {
        viewModelScope.launch {
            txProcessor.transaction.onEach {
                updateFiatExchangeRatesIfNeeded(it)
            }.onEach {
                checkIfAllowanceCanBeRevoked(it)
            }.collectLatest {
                updateState {
                    copy(
                        transaction = it
                    )
                }
            }
        }

        viewModelScope.launch {
            txProcessor.quoteFetching.mapLatest { isFetchingQuote ->
                when {
                    isFetchingQuote && modelState.operationInProgress == DexOperation.None ->
                        DexOperation.PriceFetching(amount = txProcessor.transaction.first().inputAmount!!)

                    !isFetchingQuote && modelState.operationInProgress is DexOperation.PriceFetching ->
                        DexOperation.None

                    else -> modelState.operationInProgress
                }
            }.collectLatest { operation ->
                updateState {
                    copy(
                        operationInProgress = operation
                    )
                }
            }
        }
    }

    /*
    * Revoke Allowance is used only in debug builds
    * */
    private suspend fun checkIfAllowanceCanBeRevoked(transaction: DexTransaction) {
        if (transaction.sourceAccount.currency.isLayer2Token && enviromentConfig.isRunningInDebugMode()) {
            val allowanceOutcome = dexAllowanceService.tokenAllowance(transaction.sourceAccount.currency)
            (allowanceOutcome as? Outcome.Success)?.value?.let { allowance ->
                if (allowance.isTokenAllowed) {
                    updateState {
                        copy(
                            canRevokeAllowance = true
                        )
                    }
                } else {
                    updateState {
                        copy(
                            canRevokeAllowance = false
                        )
                    }
                }
            }
        } else {
            updateState {
                copy(
                    canRevokeAllowance = false
                )
            }
        }
    }

    private fun updateFiatExchangeRatesIfNeeded(dexTransaction: DexTransaction) {
        if (modelState.sellCurrencyToFiatExchangeRate?.from != dexTransaction.sourceAccount.currency) {
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
            if (modelState.buyCurrencyToFiatExchangeRate?.from != it) {
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
                    updateState {
                        copy(
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
                    updateState {
                        copy(
                            sellCurrencyToFiatExchangeRate = rate
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
                            buyCurrencyToFiatExchangeRate = rate
                        )
                    }
                }
            }
        }
    }
}

sealed class InputAmountViewState : ViewState {
    data class TransactionInputState(
        val selectedNetwork: DataResource<DexNetworkViewState>,
        val allowNetworkSelection: Boolean,
        val sourceCurrency: Currency?,
        val destinationCurrency: Currency?,
        val maxSourceAmount: Money?,
        val sellAmount: Money?,
        val operationInProgress: DexOperation,
        val destinationAccountBalance: Money?,
        val sourceAccountBalance: Money?,
        val sellExchangeAmount: Money?,
        val buyExchangeAmount: Money?,
        val buyAmount: Money?,
        val allowanceCanBeRevoked: Boolean,
        val uiFee: UiNetworkFee,
        val previewActionButtonState: ActionButtonState,
        val sourceAccountHasNoFunds: Boolean,
        private val errors: List<DexUiError> = emptyList()
    ) : InputAmountViewState() {
        fun canChangeInputCurrency() =
            operationInProgress != DexOperation.PushingAllowance &&
                operationInProgress !is DexOperation.PollingAllowance && !sourceAccountHasNoFunds

        val allowanceTransactionInProgress: Boolean
            get() = operationInProgress == DexOperation.BuildingAllowance ||
                operationInProgress == DexOperation.PushingAllowance ||
                operationInProgress is DexOperation.PollingAllowance

        val noTokenAllowanceError: DexUiError.TokenNotAllowed?
            get() = errors.filterIsInstance<DexUiError.TokenNotAllowed>().firstOrNull()

        val alertError: AlertError?
            get() = errors.filterIsInstance<AlertError>().firstOrNull()
    }

    data class NoInputViewState(
        val selectedNetwork: DataResource<DexNetworkViewState>,
        val allowNetworkSelection: Boolean,
    ) : InputAmountViewState()

    data class NotEligible(val reason: String?) : InputAmountViewState()

    object Loading : InputAmountViewState()
}

sealed class UiNetworkFee {
    class DefinedFee(
        val feeInFiat: Money?,
        val fee: Money
    ) : UiNetworkFee()

    data class PlaceholderFee(val defFee: Money) : UiNetworkFee()

    val uiText: String
        get() = when (this) {
            is DefinedFee -> "~ ${feeInFiat?.toStringWithSymbol() ?: fee.toStringWithSymbol()}"
            is PlaceholderFee -> "~ ${defFee.toStringWithSymbol()}"
        }

    val textColor: ComposeColors
        get() = when (this) {
            is DefinedFee -> ComposeColors.Title
            is PlaceholderFee -> ComposeColors.Body
        }
}

data class AmountModelState(
    val networks: DataResource<List<CoinNetwork>> = DataResource.Loading,
    val selectedChain: Int? = null,
    val dexAllowanceState: DataResource<DexAllowanceState> = DataResource.Loading,
    val transaction: DexTransaction?,
    val ignoredTxErrors: List<DexUiError> = emptyList(),
    val operationInProgress: DexOperation = DexOperation.None,
    val sellCurrencyToFiatExchangeRate: ExchangeRate?,
    val buyCurrencyToFiatExchangeRate: ExchangeRate?,
    val feeToFiatExchangeRate: ExchangeRate?,
    val canRevokeAllowance: Boolean = false
) : ModelState

sealed class AmountNavigationEvent : NavigationEvent {
    data class ApproveAllowanceTx(val data: AllowanceTxUiData) : AmountNavigationEvent()
    data class AllowanceTxFailed(val currencyTicker: String, val reason: AllowanceFailedReason) :
        AmountNavigationEvent()

    data class AllowanceTxCompleted(val currencyTicker: String) :
        AmountNavigationEvent()

    object ReceiveOnAccount : AmountNavigationEvent()

    data class AllowanceTxUrl(val url: String) :
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
    object RevalidateTransaction : InputAmountIntent()
    object UnSubscribeToTxUpdates : InputAmountIntent()
    class ViewAllowanceTx(val currency: AssetInfo, val txId: String) : InputAmountIntent()
    object AllowanceTransactionApproved : InputAmountIntent()
    object AllowanceTransactionDeclined : InputAmountIntent()
    object BuildAllowanceTransaction : InputAmountIntent()
    object RevokeSourceCurrencyAllowance : InputAmountIntent()
    data class ReceiveCurrencyRequested(val currency: Currency) : InputAmountIntent()

    object DepositOnSourceAccountRequested : InputAmountIntent()
    data class DepositOnAccountRequested(val account: CryptoNonCustodialAccount) : InputAmountIntent()
    data class PollForPendingAllowance(val currency: AssetInfo) : InputAmountIntent() {
        override fun isValidFor(modelState: AmountModelState): Boolean {
            return modelState.operationInProgress !is DexOperation.PollingAllowance
        }
    }

    class InputAmountUpdated(val amountString: String) : InputAmountIntent()
    class OutputAmountUpdated(val amountString: String) : InputAmountIntent()
}

sealed class DexOperation {
    object None : DexOperation()

    /**
     * the amount that price is requested for
     */
    data class PriceFetching(val amount: ExchangeAmount) : DexOperation()
    object PushingAllowance : DexOperation()
    data class PollingAllowance(
        val asset: AssetInfo,
        val txId: String?
    ) : DexOperation()

    object BuildingAllowance : DexOperation()
}

@kotlinx.serialization.Serializable
data class AllowanceTxUiData(
    val currencyTicker: String,
    val networkNativeAssetTicker: String,
    val feesCrypto: String,
    val feesFiat: String,
)

enum class ActionButtonState {
    INVISIBLE, ENABLED, DISABLED
}

sealed class DexAllowanceState {
    object Allowed : DexAllowanceState()
    object NoFundsAvailable : DexAllowanceState()
    data class NotEligible(val reason: String?) : DexAllowanceState()
}
