package com.blockchain.transactions.swap.enteramount

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.componentlib.control.CurrencyValue
import com.blockchain.componentlib.control.InputCurrency
import com.blockchain.componentlib.keyboard.KeyboardButton
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResourceFlows
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
import com.blockchain.outcome.toDataResource
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.transactions.common.CombinedSourceNetworkFees
import com.blockchain.transactions.common.CryptoAccountWithBalance
import com.blockchain.transactions.common.OnChainDepositEngineInteractor
import com.blockchain.transactions.common.OnChainDepositInputValidationError
import com.blockchain.transactions.swap.SwapService
import com.blockchain.utils.removeLeadingZeros
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.CurrencyType
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.balance.isLayer2Token
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.regex.Pattern
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SwapEnterAmountViewModel(
    private val args: SwapEnterAmountArgs,
    private val swapService: SwapService,
    private val exchangeRates: ExchangeRatesDataManager,
    private val walletModeService: WalletModeService,
    private val tradeDataService: TradeDataService,
    private val assetCatalogue: AssetCatalogue,
    private val onChainDepositEngineInteractor: OnChainDepositEngineInteractor,
    currencyPrefs: CurrencyPrefs
) : MviViewModel<
    SwapEnterAmountIntent,
    SwapEnterAmountViewState,
    SwapEnterAmountModelState,
    SwapEnterAmountNavigationEvent,
    ModelConfigArgs.NoArgs
    >(
    SwapEnterAmountModelState()
) {

    private var configJob: Job? = null
    private val fiatInputChanges = MutableSharedFlow<FiatValue>()
    private val cryptoInputChanges = MutableSharedFlow<CryptoValue>()

    private val fiatCurrency: FiatCurrency by lazy {
        currencyPrefs.selectedFiatCurrency
    }

    init {
        viewModelScope.launch {
            val walletMode = walletModeService.walletMode.firstOrNull()
            val argsSourceAccount = args.sourceAccount.data

            val fromAccountWithBalance = if (argsSourceAccount != null) {
                val balance = argsSourceAccount.balance().firstOrNull()
                val fromAccount = balance?.let {
                    CryptoAccountWithBalance(
                        account = argsSourceAccount,
                        balanceCrypto = balance.total as CryptoValue,
                        balanceFiat = balance.totalFiat as FiatValue,
                    )
                }
                fromAccount
            } else {
                // get highest balance account as the primary FROM
                swapService.highestBalanceSourceAccount()
            }

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

    override fun SwapEnterAmountModelState.reduce(): SwapEnterAmountViewState {
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
            sourceNetworkFees: $sourceNetworkFees
            targetNetworkFeeInSourceValue: $targetNetworkFeeInSourceValue
            depositEngineInputValidationError: $depositEngineInputValidationError
            fiatAmount: $fiatAmount
            fiatAmountUserInput: $fiatAmountUserInput
            cryptoAmount: $cryptoAmount
            cryptoAmountUserInput: $cryptoAmountUserInput
            selectedInput: $selectedInput
            inputError: ${validateAmount()}
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
        val inputError = if (fiatAmountUserInput.isNotEmpty() && cryptoAmountUserInput.isNotEmpty()) {
            validateAmount()
        } else {
            null
        }
        return SwapEnterAmountViewState(
            selectedInput = when (selectedInput) {
                CurrencyType.FIAT -> InputCurrency.Currency1
                CurrencyType.CRYPTO -> InputCurrency.Currency2
            },
            assets = fromAccount?.let {
                EnterAmountAssets(
                    from = fromAccount.account.toViewState(),
                    to = toAccount?.toViewState()
                )
            },
            maxAmount = currencyAwareMaxAmount?.toStringWithSymbol(),
            fiatAmount = CurrencyValue(
                value = if (selectedInput == CurrencyType.FIAT) {
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
                zeroHint = "0"
            ),
            cryptoAmount = fromAccount?.let {
                CurrencyValue(
                    value = if (selectedInput == CurrencyType.CRYPTO) {
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
            inputError = inputError,
            // If there's an input error, most likely the quote/price endpoint or the deposit engine will fail in some way
            // therefore we're giving preference to showing the inputError rather than the snackbarError
            snackbarError = (getDepositNetworkFeeError ?: getTargetNetworkFeeError).takeIf { inputError == null },
            fatalError = fatalError
        )
    }

    override suspend fun handleIntent(modelState: SwapEnterAmountModelState, intent: SwapEnterAmountIntent) {
        when (intent) {
            SwapEnterAmountIntent.FlipInputs -> {
                updateState {
                    copy(
                        selectedInput = when (selectedInput) {
                            CurrencyType.CRYPTO -> CurrencyType.FIAT
                            CurrencyType.FIAT -> CurrencyType.CRYPTO
                        }
                    )
                }
            }

            is SwapEnterAmountIntent.KeyboardClicked -> {
                val currentInputType = modelState.selectedInput
                val currentInput = when (currentInputType) {
                    CurrencyType.CRYPTO -> modelState.cryptoAmountUserInput
                    CurrencyType.FIAT -> modelState.fiatAmountUserInput
                }
                val currentInputCurrencyMaxFractionDigits = when (currentInputType) {
                    CurrencyType.CRYPTO -> modelState.cryptoAmount?.userDecimalPlaces ?: 8
                    CurrencyType.FIAT -> modelState.fiatAmount?.userDecimalPlaces ?: 2
                }
                val newInput = when (intent.button) {
                    KeyboardButton.Backspace -> currentInput.dropLast(1)
                    is KeyboardButton.Value -> currentInput + intent.button.value
                    KeyboardButton.Biometrics,
                    KeyboardButton.None -> throw UnsupportedOperationException()
                }.let { newInput ->
                    val decimalSeparator = DecimalFormatSymbols(Locale.getDefault()).decimalSeparator.toString()
                    if (newInput.startsWith(decimalSeparator)) "0$newInput"
                    else newInput
                }

                if (!validateInput(newInput, currentInputCurrencyMaxFractionDigits)) return

                when (currentInputType) {
                    CurrencyType.FIAT -> fiatInputChanged(newInput)
                    CurrencyType.CRYPTO -> cryptoInputChanged(newInput)
                }
            }

            is SwapEnterAmountIntent.FromAccountChanged -> {
                check(modelState.walletMode != null)
                updateState {
                    copy(config = DataResource.Loading)
                }

                // verify if current TO account is still valid
                val isToAccountStillValid = modelState.toAccount?.let {
                    swapService.isTargetAccountValidForSource(
                        targetAccount = it,
                        sourceTicker = intent.account.account.currency.networkTicker,
                        mode = modelState.walletMode
                    )
                } ?: false
                val toAccount = modelState.toAccount?.takeIf { isToAccountStillValid }
                    ?: swapService.bestTargetAccountForMode(
                        sourceTicker = intent.account.account.currency.networkTicker,
                        targetTicker = null,
                        mode = modelState.walletMode
                    )

                updateState {
                    copy(
                        fromAccount = intent.account,
                        secondPassword = intent.secondPassword,
                        toAccount = toAccount,
                        fiatAmount = null,
                        fiatAmountUserInput = "",
                        cryptoAmount = null,
                        cryptoAmountUserInput = "",
                        sourceNetworkFees = null,
                        targetNetworkFeeInSourceValue = null,
                        fatalError = null,
                    )
                }

                updateConfig(intent.account.account, toAccount, null)
            }

            is SwapEnterAmountIntent.ToAccountChanged -> {
                val fromAccount = modelState.fromAccount?.account
                check(fromAccount != null)
                updateState {
                    copy(
                        toAccount = intent.account,
                        config = DataResource.Loading,
                        targetNetworkFeeInSourceValue = null,
                    )
                }

                updateConfig(fromAccount, intent.account, modelState.cryptoAmount)
            }

            SwapEnterAmountIntent.MaxSelected -> {
                // We're ignore the [selectedInput] and always changing the cryptoInput so we make sure to swap everything the user has
                // and not leave the user with 0.0000000012 of some currency, this is because the Fiat currency has lower precision and
                // will never be converted correctly back to crypto and match the true max.
                cryptoInputChanged(modelState.maxLimit.toInputString())
            }

            SwapEnterAmountIntent.PreviewClicked -> {
                val fromAccount = modelState.fromAccount?.account
                check(fromAccount != null)
                val toAccount = modelState.toAccount
                check(toAccount != null)
                val cryptoAmount = modelState.cryptoAmount
                check(cryptoAmount != null)

                navigate(
                    SwapEnterAmountNavigationEvent.Preview(
                        sourceAccount = fromAccount,
                        targetAccount = toAccount,
                        sourceCryptoAmount = cryptoAmount,
                        secondPassword = modelState.secondPassword
                    )
                )
            }

            SwapEnterAmountIntent.SnackbarErrorHandled -> {
                updateState { copy(getDepositNetworkFeeError = null, getTargetNetworkFeeError = null) }
            }
        }
    }

    private suspend fun fiatInputChanged(newInput: String) {
        val fiatCurrency = fiatCurrency
        val fiatAmount = newInput.takeIf { it.isNotEmpty() }
            ?.let { FiatValue.fromMajor(fiatCurrency, it.toBigDecimal()) }
            ?: FiatValue.zero(fiatCurrency)

        val cryptoAmount: CryptoValue? = modelState.config.map { it.sourceAccountToFiatRate }.dataOrElse(null)
            ?.inverse()
            ?.convert(fiatAmount) as CryptoValue?

        updateState {
            copy(
                fiatAmountUserInput = newInput.removeLeadingZeros(),
                fiatAmount = fiatAmount,
                cryptoAmountUserInput = cryptoAmount.toInputString(),
                cryptoAmount = cryptoAmount,
            )
        }

        fiatInputChanges.emit(fiatAmount)
    }

    private suspend fun cryptoInputChanged(newInput: String) {
        val fromAccount = modelState.fromAccount?.account
        val fromCurrency = fromAccount?.currency
        check(fromCurrency != null)

        val cryptoAmount = newInput.takeIf { it.isNotEmpty() }
            ?.let { CryptoValue.fromMajor(fromCurrency, it.toBigDecimal()) }
            ?: CryptoValue.zero(fromAccount.currency)

        val fiatAmount: FiatValue? = modelState.config.map { it.sourceAccountToFiatRate }.dataOrElse(null)
            ?.convert(cryptoAmount) as FiatValue?

        updateState {
            copy(
                cryptoAmountUserInput = newInput.removeLeadingZeros(),
                cryptoAmount = cryptoAmount,
                fiatAmountUserInput = fiatAmount.toInputString(),
                fiatAmount = fiatAmount
            )
        }

        cryptoInputChanges.emit(cryptoAmount)
    }

    private var getSourceNetworkFeeJob: Job? = null
    private fun getSourceNetworkFeeAndDepositEngineInputValidation(
        sourceAccount: CryptoNonCustodialAccount,
        targetAccount: CryptoAccount,
        amount: CryptoValue,
    ) {
        getSourceNetworkFeeJob?.cancel()
        getSourceNetworkFeeJob = viewModelScope.launch {
            updateState { copy(depositEngineInputValidationError = null, getDepositNetworkFeeError = null) }
            onChainDepositEngineInteractor.getDepositNetworkFee(AssetAction.Swap, sourceAccount, targetAccount, amount)
                .doOnSuccess { fees ->
                    updateState {
                        copy(sourceNetworkFees = fees)
                    }
                }
                .doOnFailure { error ->
                    updateState { copy(getDepositNetworkFeeError = error) }
                }
                .flatMap {
                    onChainDepositEngineInteractor.validateAmount(
                        AssetAction.Swap,
                        sourceAccount,
                        targetAccount,
                        amount
                    ).doOnFailure { error ->
                        updateState { copy(depositEngineInputValidationError = error) }
                    }
                }
        }
    }

    private var targetNetworkFeeRefreshingJob: Job? = null
    private fun startTargetNetworkFeeRefreshing(
        sourceAccount: CryptoAccount,
        targetAccount: CryptoAccount,
        amount: CryptoValue,
    ) {
        val refreshDelay = 10_000L
        val pair = CurrencyPair(sourceAccount.currency, targetAccount.currency)
        val direction = getTransferDirection(sourceAccount, targetAccount)

        targetNetworkFeeRefreshingJob?.cancel()
        targetNetworkFeeRefreshingJob = viewModelScope.launch {
            while (true) {
                updateState { copy(getTargetNetworkFeeError = null) }
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
                                targetNetworkFeeInSourceValue = targetNetworkFeeInSourceValue,
                            )
                        }
                    }
                    .doOnFailure { error ->
                        updateState { copy(getTargetNetworkFeeError = error) }
                    }

                delay(refreshDelay)
            }
        }
    }

    private fun updateConfig(fromAccount: CryptoAccount, toAccount: CryptoAccount?, cryptoAmount: CryptoValue?) {
        configJob?.cancel()
        getSourceNetworkFeeJob?.cancel()
        targetNetworkFeeRefreshingJob?.cancel()

        if (toAccount == null) return

        if (fromAccount is CryptoNonCustodialAccount) {
            val amount = cryptoAmount ?: CryptoValue.zero(fromAccount.currency)
            getSourceNetworkFeeAndDepositEngineInputValidation(fromAccount, toAccount, amount)
            if (toAccount is CryptoNonCustodialAccount) {
                startTargetNetworkFeeRefreshing(fromAccount, toAccount, amount)
            } else {
                updateState { copy(targetNetworkFeeInSourceValue = CryptoValue.zero(fromAccount.currency)) }
            }
        } else {
            updateState {
                copy(
                    sourceNetworkFees = CombinedSourceNetworkFees.zero(fromAccount.currency),
                    targetNetworkFeeInSourceValue = CryptoValue.zero(fromAccount.currency)
                )
            }
        }

        configJob = viewModelScope.launch {
            val limitsFlow = flow {
                val limits = swapService.limits(
                    from = fromAccount.currency as CryptoCurrency,
                    to = toAccount.currency as CryptoCurrency,
                    fiat = fiatCurrency,
                    direction = getTransferDirection(fromAccount, toAccount),
                )
                emit(limits.toDataResource())
            }
            combineDataResourceFlows(
                exchangeRates.exchangeRateToUserFiatFlow(fromAccount.currency),
                limitsFlow
            ) { exchangeRate, limits ->
                EnterAmountConfig(
                    sourceAccountToFiatRate = exchangeRate,
                    productLimits = limits
                )
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
    private fun SwapEnterAmountModelState.validateAmount(): SwapEnterAmountInputError? {
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
            CurrencyType.FIAT -> spendableBalanceFiat
            CurrencyType.CRYPTO -> spendableBalance
        }?.toStringWithSymbol() ?: "-"
        val minLimitString = when (selectedInput) {
            CurrencyType.FIAT -> minLimitFiat
            CurrencyType.CRYPTO -> minLimit
        }?.toStringWithSymbol() ?: "-"
        val maxLimitString = when (selectedInput) {
            CurrencyType.FIAT -> maxLimitFiat
            CurrencyType.CRYPTO -> maxLimit
        }?.toStringWithSymbol() ?: "-"

        return when {
            spendableBalance != null && cryptoAmount > spendableBalance -> {
                SwapEnterAmountInputError.AboveBalance(
                    displayTicker = spendableBalance.currency.displayTicker,
                    balance = spendableBalanceString,
                )
            }

            minLimit != null && cryptoAmount < minLimit -> {
                SwapEnterAmountInputError.BelowMinimum(
                    minValue = minLimitString,
                    direction = safeLet(
                        fromAccount?.account,
                        toAccount
                    ) { source, target ->
                        getTransferDirection(source, target)
                    } ?: TransferDirection.INTERNAL
                )
            }

            maxLimit != null && cryptoAmount > maxLimit -> {
                SwapEnterAmountInputError.AboveMaximum(maxValue = maxLimitString)
            }

            else -> when (val error = depositEngineInputValidationError) {
                OnChainDepositInputValidationError.InsufficientFunds -> SwapEnterAmountInputError.AboveBalance(
                    displayTicker = spendableBalance?.currency?.displayTicker ?: "-",
                    balance = spendableBalanceString,
                )

                OnChainDepositInputValidationError.InsufficientGas -> {
                    val asset = (sourceNetworkFees?.feeForAmount ?: spendableBalance)?.currency
                    SwapEnterAmountInputError.InsufficientGas(
                        displayTicker = asset?.displayTicker ?: "-"
                    )
                }
                is OnChainDepositInputValidationError.Unknown -> SwapEnterAmountInputError.Unknown(error.error)
                null -> null
            }
        }
    }

    private fun validateInput(input: String, maxFractionDigits: Int): Boolean {
        val pattern = let {
            val decimalSeparator = DecimalFormatSymbols(Locale.getDefault()).decimalSeparator.toString()
            Pattern.compile(
                "-?\\d{0,100}+((\\$decimalSeparator\\d{0,$maxFractionDigits})?)||(\\$decimalSeparator)?"
            )
        }

        return pattern.matcher(input).matches()
    }

    private fun getTransferDirection(sourceAccount: CryptoAccount, targetAccount: CryptoAccount): TransferDirection =
        when {
            sourceAccount is NonCustodialAccount && targetAccount is NonCustodialAccount -> TransferDirection.ON_CHAIN
            sourceAccount is NonCustodialAccount -> TransferDirection.FROM_USERKEY
            // TransferDirection.FROM_USERKEY not supported
            targetAccount is NonCustodialAccount -> throw UnsupportedOperationException()
            else -> TransferDirection.INTERNAL
        }

    private fun SingleAccount.toViewState() = EnterAmountAssetState(
        iconUrl = currency.logo,
        nativeAssetIconUrl = (this as? CryptoNonCustodialAccount)?.currency
            ?.takeIf { it.isLayer2Token }
            ?.coinNetwork?.nativeAssetTicker
            ?.let { assetCatalogue.fromNetworkTicker(it)?.logo },
        ticker = currency.displayTicker,
    )

    companion object {
        private const val VALIDATION_DEBOUNCE_MS = 400L
    }
}

private val SwapEnterAmountModelState.currencyAwareMaxAmount: Money?
    get() = when (selectedInput) {
        CurrencyType.FIAT -> safeLet(
            config.dataOrNull()?.sourceAccountToFiatRate,
            maxLimit,
        ) { rate, limit ->
            rate.convert(limit, RoundingMode.FLOOR)
        }

        CurrencyType.CRYPTO -> maxLimit
    }

private fun initialTargetAccountRule(fromTicker: String) = when (fromTicker) {
    "BTC" -> "USDT"
    else -> "BTC"
}

private fun Money?.toInputString(): String = this?.toBigDecimal()
    ?.setScale(this.userDecimalPlaces, RoundingMode.FLOOR)
    ?.stripTrailingZeros()
    ?.takeIf { it != BigDecimal.ZERO }
    ?.toPlainString()
    .orEmpty()
