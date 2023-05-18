package com.blockchain.transactions.swap.enteramount

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.componentlib.control.CurrencyValue
import com.blockchain.componentlib.control.InputCurrency
import com.blockchain.componentlib.control.flip
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResources
import com.blockchain.data.dataOrElse
import com.blockchain.data.dataOrNull
import com.blockchain.data.map
import com.blockchain.data.updateDataWith
import com.blockchain.domain.trade.TradeDataService
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.extensions.safeLet
import com.blockchain.logging.Logger
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.flatMap
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.transactions.common.OnChainDepositEngineInteractor
import com.blockchain.transactions.common.OnChainDepositInputValidationError
import com.blockchain.transactions.swap.SwapService
import com.blockchain.transactions.swap.confirmation.SwapConfirmationArgs
import com.blockchain.utils.removeLeadingZeros
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * @property fromTicker if we come from Coinview we should have preset FROM
 */
@OptIn(FlowPreview::class)
class EnterAmountViewModel(
    private val fromTicker: String? = null,
    private val swapService: SwapService,
    private val exchangeRates: ExchangeRatesDataManager,
    private val walletModeService: WalletModeService,
    private val tradeDataService: TradeDataService,
    private val onChainDepositEngineInteractor: OnChainDepositEngineInteractor,
    currencyPrefs: CurrencyPrefs,
    private val confirmationArgs: SwapConfirmationArgs
) : MviViewModel<
    EnterAmountIntent,
    EnterAmountViewState,
    EnterAmountModelState,
    EnterAmountNavigationEvent,
    ModelConfigArgs.NoArgs
    >(
    EnterAmountModelState(fiatCurrency = currencyPrefs.selectedFiatCurrency)
) {

    private var configJob: Job? = null
    private val fiatInputChanges = MutableSharedFlow<FiatValue>()
    private val cryptoInputChanges = MutableSharedFlow<CryptoValue>()

    init {
        viewModelScope.launch {
            val walletMode = walletModeService.walletMode.firstOrNull()

            // get highest balance account as the primary FROM
            val fromAccountWithBalance = swapService.highestBalanceSourceAccount()
            val fromAccount = fromAccountWithBalance?.account

            // if no account found -> fatal error loading accounts
            if (fromAccount == null || walletMode == null) {
                updateState {
                    copy(
                        fatalError = SwapEnterAmountFatalError.WalletLoading
                    )
                }
                return@launch
            }

            val fromAccountTicker = fromAccount.currency.networkTicker
            val toAccount = swapService.bestTargetAccountForMode(
                sourceTicker = fromAccountTicker,
                targetTicker = initialTargetAccountRule(fromAccountTicker),
                mode = walletMode
            )

            updateState {
                copy(
                    fromAccount = fromAccountWithBalance,
                    toAccount = toAccount,
                    walletMode = walletMode,
                )
            }

            updateConfig(fromAccount, toAccount, null)
        }

        viewModelScope.launch {
            fiatInputChanges.debounce(VALIDATION_DEBOUNCE_MS)
                .collectLatest {
                    refreshNetworkFeesAndDepositEngineInputValidation()
                }
        }

        viewModelScope.launch {
            cryptoInputChanges.debounce(VALIDATION_DEBOUNCE_MS)
                .collectLatest {
                    refreshNetworkFeesAndDepositEngineInputValidation()
                }
        }
    }

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun EnterAmountModelState.reduce(): EnterAmountViewState {
        val rate = config.dataOrNull()?.sourceAccountToFiatRate
        val toUserFiat: CryptoValue.() -> FiatValue? = {
            rate?.convert(this) as FiatValue?
        }
        Logger.e(
            """
            walletMode: $walletMode
            fromAccount: $fromAccount
            toAccount: $toAccount
            fiatCurrency: $fiatCurrency
            config: $config
            sourceToTargetExchangeRate: $sourceToTargetExchangeRate
            sourceNetworkFee: $sourceNetworkFee
            targetNetworkFeeInSourceValue: $targetNetworkFeeInSourceValue
            depositEngineInputValidationError: $depositEngineInputValidationError
            fiatAmount: $fiatAmount
            fiatAmountUserInput: $fiatAmountUserInput
            cryptoAmount: $cryptoAmount
            cryptoAmountUserInput: $cryptoAmountUserInput
            selectedInput: $selectedInput
            inputError: ${validateInput()}
            fatalError: $fatalError
            minLimit: ${
            try {
                minLimit
            } catch (ex: Exception) {
                ex
            }
            }
            minLimitFiat: ${
            try {
                minLimit?.toUserFiat()
            } catch (ex: Exception) {
                ex
            }
            }
            maxLimit: ${
            try {
                maxLimit
            } catch (ex: Exception) {
                ex
            }
            }
            maxLimitFiat: ${
            try {
                maxLimit?.toUserFiat()
            } catch (ex: Exception) {
                ex
            }
            }
            """.trimIndent()
        )
        return EnterAmountViewState(
            selectedInput = selectedInput,
            assets = fromAccount?.let {
                EnterAmountAssets(
                    from = fromAccount.account.currency.toViewState(),
                    to = toAccount?.currency?.toViewState()
                )
            },
            maxAmount = currencyAwareMaxAmount?.toStringWithSymbol(),
            fiatAmount = CurrencyValue(
                value = if (selectedInput == InputCurrency.Currency1) {
                    fiatAmountUserInput
                } else {
                    // fiatAmountUserInput.ifEmpty { "0" }
                    // format the unfocused value?
                    fiatAmount?.toStringWithoutSymbol().orEmpty().ifEmpty { "0" }
                },
                maxFractionDigits = fiatAmount?.userDecimalPlaces ?: 2,
                ticker = fiatCurrency.symbol,
                isPrefix = true,
                separateWithSpace = false,
                zeroHint = "0.00"
            ),
            cryptoAmount = fromAccount?.let {
                CurrencyValue(
                    value = if (selectedInput == InputCurrency.Currency2) {
                        cryptoAmountUserInput
                    } else {
                        // cryptoAmountUserInput.ifEmpty { "0" }
                        // format the unfocused value?
                        cryptoAmount?.toStringWithoutSymbol().orEmpty().ifEmpty { "0" }
                    },
                    maxFractionDigits = cryptoAmount?.userDecimalPlaces ?: 8,
                    ticker = fromAccount.account.currency.displayTicker,
                    isPrefix = false,
                    separateWithSpace = true,
                    zeroHint = "0"
                )
            },
            inputError = if (fiatAmountUserInput.isNotEmpty() && cryptoAmountUserInput.isNotEmpty()) {
                validateInput()
            } else {
                null
            },
            snackbarError = snackbarError,
            fatalError = fatalError
        )
    }

    override suspend fun handleIntent(modelState: EnterAmountModelState, intent: EnterAmountIntent) {
        when (intent) {
            EnterAmountIntent.FlipInputs -> {
                updateState {
                    copy(
                        selectedInput = selectedInput.flip()
                    )
                }
            }

            is EnterAmountIntent.FiatInputChanged -> {
                val fiatCurrency = modelState.fiatCurrency
                val fiatAmount = intent.amount.takeIf { it.isNotEmpty() }
                    ?.let { FiatValue.fromMajor(fiatCurrency, it.toBigDecimal()) }
                    ?: FiatValue.zero(fiatCurrency)

                val cryptoAmount: CryptoValue? = modelState.config.map { it.sourceAccountToFiatRate }.dataOrElse(null)
                    ?.inverse()
                    ?.convert(fiatAmount) as CryptoValue?

                updateState {
                    copy(
                        fiatAmountUserInput = intent.amount.removeLeadingZeros(),
                        fiatAmount = fiatAmount,
                        cryptoAmountUserInput = cryptoAmount.toInputString(),
                        cryptoAmount = cryptoAmount,
                    )
                }

                fiatInputChanges.emit(fiatAmount)
            }

            is EnterAmountIntent.CryptoInputChanged -> {
                val fromAccount = modelState.fromAccount?.account
                val fromCurrency = fromAccount?.currency
                check(fromCurrency != null)

                val cryptoAmount = intent.amount.takeIf { it.isNotEmpty() }
                    ?.let { CryptoValue.fromMajor(fromCurrency, it.toBigDecimal()) }
                    ?: CryptoValue.zero(fromAccount.currency)

                val fiatAmount: FiatValue? = modelState.config.map { it.sourceAccountToFiatRate }.dataOrElse(null)
                    ?.convert(cryptoAmount) as FiatValue?

                updateState {
                    copy(
                        cryptoAmountUserInput = intent.amount.removeLeadingZeros(),
                        cryptoAmount = cryptoAmount,
                        fiatAmountUserInput = fiatAmount.toInputString(),
                        fiatAmount = fiatAmount
                    )
                }

                cryptoInputChanges.emit(cryptoAmount)
            }

            is EnterAmountIntent.FromAccountChanged -> {
                check(modelState.walletMode != null)

                // verify if current TO account is still valid
                val isToAccountStillValid = modelState.toAccount?.let {
                    swapService.isAccountValidForSource(
                        account = it,
                        sourceTicker = intent.account.account.currency.networkTicker,
                        mode = modelState.walletMode
                    )
                } ?: false
                val toAccount = modelState.toAccount?.takeIf { isToAccountStillValid }

                updateState {
                    copy(
                        fromAccount = intent.account,
                        secondPassword = intent.secondPassword,
                        toAccount = toAccount,
                        fiatAmount = null,
                        fiatAmountUserInput = "",
                        cryptoAmount = null,
                        cryptoAmountUserInput = "",
                        sourceToTargetExchangeRate = null,
                        sourceNetworkFee = null,
                        targetNetworkFeeInSourceValue = null,
                        fatalError = null,
                    )
                }

                updateConfig(intent.account.account, toAccount, null)
            }

            is EnterAmountIntent.ToAccountChanged -> {
                val fromAccount = modelState.fromAccount?.account
                check(fromAccount != null)
                updateState {
                    copy(
                        toAccount = intent.account,
                        sourceToTargetExchangeRate = null,
                        targetNetworkFeeInSourceValue = null,
                    )
                }

                updateConfig(fromAccount, intent.account, modelState.cryptoAmount)
            }

            EnterAmountIntent.MaxSelected -> {
                val userInputBalance = modelState.currencyAwareMaxAmount.toInputString()

                when (modelState.selectedInput) {
                    InputCurrency.Currency1 -> {
                        onIntent(EnterAmountIntent.FiatInputChanged(amount = userInputBalance))
                    }

                    InputCurrency.Currency2 -> {
                        onIntent(EnterAmountIntent.CryptoInputChanged(amount = userInputBalance))
                    }
                }
            }

            EnterAmountIntent.PreviewClicked -> {
                val fromAccount = modelState.fromAccount?.account
                check(fromAccount != null)
                val toAccount = modelState.toAccount
                check(toAccount != null)
                val cryptoAmount = modelState.cryptoAmount
                check(cryptoAmount != null)

                confirmationArgs.update(
                    sourceAccount = fromAccount,
                    targetAccount = toAccount,
                    sourceCryptoAmount = cryptoAmount,
                    secondPassword = modelState.secondPassword
                )
                navigate(EnterAmountNavigationEvent.Preview)
            }

            EnterAmountIntent.SnackbarErrorHandled -> {
                updateState { copy(snackbarError = null) }
            }
        }
    }

    private var getSourceNetworkFeeJob: Job? = null
    private fun getSourceNetworkFeeAndDepositEngineInputValidation(
        sourceAccount: CryptoNonCustodialAccount,
        targetAccount: CryptoAccount,
        amount: CryptoValue,
    ) {
        getSourceNetworkFeeJob?.cancel()
        getSourceNetworkFeeJob = viewModelScope.launch {
            onChainDepositEngineInteractor.getDepositNetworkFee(sourceAccount, targetAccount, amount)
                .doOnSuccess { fee ->
                    updateState {
                        copy(sourceNetworkFee = fee)
                    }
                }
                .doOnFailure { error ->
                    updateState { copy(snackbarError = error) }
                }
                .flatMap {
                    onChainDepositEngineInteractor.validateAmount(sourceAccount, targetAccount, amount)
                        .doOnFailure { error ->
                            updateState { copy(depositEngineInputValidationError = error) }
                        }
                }
        }
    }

    private var quotePriceRefreshingJob: Job? = null
    private fun startTargetNetworkFeeRefreshing(
        sourceAccount: CryptoAccount,
        targetAccount: CryptoAccount,
        amount: CryptoValue,
    ) {
        val refreshDelay = 10_000L
        val pair = CurrencyPair(sourceAccount.currency, targetAccount.currency)
        val direction = getTransferDirection(sourceAccount, targetAccount)

        quotePriceRefreshingJob?.cancel()
        quotePriceRefreshingJob = viewModelScope.launch {
            while (true) {
                tradeDataService.getSwapQuotePrice(pair, amount, direction)
                    .doOnSuccess { quotePrice ->
                        updateState {
                            val sourceAsset = sourceAccount.currency
                            val networkFee = quotePrice.networkFee ?: Money.zero(targetAccount.currency)

                            // Source -> Target exchange rate without network fees
                            val targetNetworkFeeInSourceValue = CryptoValue.fromMajor(
                                sourceAsset,
                                networkFee.toBigDecimal().divide(
                                    quotePrice.rawPrice.toBigDecimal(),
                                    sourceAsset.precisionDp,
                                    RoundingMode.HALF_UP,
                                )
                            )

                            copy(
                                sourceToTargetExchangeRate = quotePrice.sourceToDestinationRate,
                                targetNetworkFeeInSourceValue = targetNetworkFeeInSourceValue,
                            )
                        }
                    }
                    .doOnFailure { error ->
                        updateState { copy(snackbarError = error) }
                    }

                delay(refreshDelay)
            }
        }
    }

    private fun updateConfig(fromAccount: CryptoAccount, toAccount: CryptoAccount?, cryptoAmount: CryptoValue?) {
        configJob?.cancel()
        getSourceNetworkFeeJob?.cancel()
        quotePriceRefreshingJob?.cancel()

        if (fromAccount is CryptoNonCustodialAccount && toAccount != null) {
            val amount = cryptoAmount ?: CryptoValue.zero(fromAccount.currency)
            getSourceNetworkFeeAndDepositEngineInputValidation(fromAccount, toAccount, amount)
            if (toAccount is CryptoNonCustodialAccount) {
                startTargetNetworkFeeRefreshing(fromAccount, toAccount, amount)
            }
        }

        if (toAccount == null) {
            updateState {
                copy(config = DataResource.Loading)
            }
            return
        }

        configJob = viewModelScope.launch {
            combine(
                exchangeRates.exchangeRateToUserFiatFlow(fromAccount.currency),
                swapService.limits(
                    from = fromAccount.currency as CryptoCurrency,
                    to = toAccount.currency as CryptoCurrency,
                    fiat = modelState.fiatCurrency,
                    direction = getTransferDirection(fromAccount, toAccount),
                )
            ) { exchangeRate, limits ->
                combineDataResources(
                    exchangeRate,
                    limits
                ) { exchangeRateData, limitsData ->
                    EnterAmountConfig(
                        sourceAccountToFiatRate = exchangeRateData,
                        productLimits = limitsData
                    )
                }
            }.collectLatest { configData ->
                updateState {
                    copy(
                        config = config.updateDataWith(configData)
                    )
                }
            }
        }
    }

    private fun refreshNetworkFeesAndDepositEngineInputValidation() {
        val fromAccount = modelState.fromAccount?.account
        val toAccount = modelState.toAccount
        val cryptoAmount = modelState.cryptoAmount

        if (fromAccount is CryptoNonCustodialAccount && toAccount != null && cryptoAmount != null) {
            getSourceNetworkFeeAndDepositEngineInputValidation(fromAccount, toAccount, cryptoAmount)
            if (toAccount is CryptoNonCustodialAccount) {
                startTargetNetworkFeeRefreshing(fromAccount, toAccount, cryptoAmount)
            }
        }
    }

    /**
     * update the fiat value based on the crypto value input
     * and update input errors
     */
    private fun EnterAmountModelState.validateInput(): SwapEnterAmountInputError? {
        val cryptoAmount = cryptoAmount ?: return null
        val minLimit = minLimit
        val maxLimit = maxLimit
        val spendableBalance = spendableBalance

        val spendableBalanceFiat = safeLet(
            config.dataOrNull()?.sourceAccountToFiatRate,
            spendableBalance,
        ) { rate, limit ->
            rate.convert(limit, RoundingMode.CEILING)
        }
        val minLimitFiat = safeLet(
            config.dataOrNull()?.sourceAccountToFiatRate,
            minLimit,
        ) { rate, limit ->
            rate.convert(limit, RoundingMode.CEILING)
        }
        val maxLimitFiat = safeLet(
            config.dataOrNull()?.sourceAccountToFiatRate,
            maxLimit,
        ) { rate, limit ->
            rate.convert(limit, RoundingMode.FLOOR)
        }
        val spendableBalanceString = when (selectedInput) {
            InputCurrency.Currency1 -> spendableBalanceFiat
            InputCurrency.Currency2 -> spendableBalance
        }?.toStringWithSymbol() ?: "-"
        val minLimitString = when (selectedInput) {
            InputCurrency.Currency1 -> minLimitFiat
            InputCurrency.Currency2 -> minLimit
        }?.toStringWithSymbol() ?: "-"
        val maxLimitString = when (selectedInput) {
            InputCurrency.Currency1 -> maxLimitFiat
            InputCurrency.Currency2 -> maxLimit
        }?.toStringWithSymbol() ?: "-"

        return when {
            spendableBalance != null && cryptoAmount > spendableBalance -> {
                SwapEnterAmountInputError.AboveBalance(
                    displayTicker = spendableBalance.currency.displayTicker,
                    balance = spendableBalanceString,
                )
            }

            minLimit != null && cryptoAmount < minLimit -> {
                SwapEnterAmountInputError.BelowMinimum(minValue = minLimitString)
            }

            maxLimit != null && cryptoAmount > maxLimit -> {
                SwapEnterAmountInputError.AboveMaximum(maxValue = maxLimitString)
            }

            else -> when (val error = depositEngineInputValidationError) {
                OnChainDepositInputValidationError.InsufficientFunds -> SwapEnterAmountInputError.AboveBalance(
                    displayTicker = spendableBalance?.currency?.displayTicker ?: "-",
                    balance = spendableBalanceString,
                )

                OnChainDepositInputValidationError.InsufficientGas -> SwapEnterAmountInputError.InsufficientGas(
                    displayTicker = (sourceNetworkFee ?: spendableBalance)?.currency?.displayTicker ?: "-"
                )

                is OnChainDepositInputValidationError.Unknown -> SwapEnterAmountInputError.Unknown(error.error)
                null -> null
            }
        }
    }

    private fun getTransferDirection(sourceAccount: CryptoAccount, targetAccount: CryptoAccount): TransferDirection =
        when {
            sourceAccount is NonCustodialAccount && targetAccount is NonCustodialAccount -> TransferDirection.ON_CHAIN
            sourceAccount is NonCustodialAccount -> TransferDirection.FROM_USERKEY
            // TransferDirection.FROM_USERKEY not supported
            targetAccount is NonCustodialAccount -> throw UnsupportedOperationException()
            else -> TransferDirection.INTERNAL
        }

    companion object {
        private const val VALIDATION_DEBOUNCE_MS = 400L
    }
}

private fun Currency.toViewState() = EnterAmountAssetState(
    iconUrl = logo,
    ticker = displayTicker
)

private val EnterAmountModelState.currencyAwareMaxAmount: Money?
    get() = when (selectedInput) {
        InputCurrency.Currency1 -> safeLet(
            config.dataOrNull()?.sourceAccountToFiatRate,
            maxLimit,
        ) { rate, limit ->
            rate.convert(limit, RoundingMode.FLOOR)
        }

        InputCurrency.Currency2 -> maxLimit
    }

private fun initialTargetAccountRule(fromTicker: String) = when (fromTicker) {
    "BTC" -> "USDT"
    else -> "BTC"
}

private fun Money?.toInputString(): String = this?.toBigDecimal()
    ?.setScale(this.userDecimalPlaces, RoundingMode.FLOOR)
    ?.stripTrailingZeros()
    ?.takeIf { it != BigDecimal.ZERO }
    ?.toString().orEmpty()
