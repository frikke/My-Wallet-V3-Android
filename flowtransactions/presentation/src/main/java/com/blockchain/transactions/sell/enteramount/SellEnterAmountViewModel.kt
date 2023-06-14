package com.blockchain.transactions.sell.enteramount

import androidx.lifecycle.viewModelScope
import com.blockchain.analytics.Analytics
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.componentlib.control.CurrencyValue
import com.blockchain.componentlib.control.InputCurrency
import com.blockchain.componentlib.keyboard.KeyboardButton
import com.blockchain.data.firstOutcome
import com.blockchain.domain.trade.TradeDataService
import com.blockchain.domain.trade.model.QuickFillRoundingData
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.extensions.safeLet
import com.blockchain.logging.Logger
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.getOrDefault
import com.blockchain.presentation.complexcomponents.QuickFillButtonData
import com.blockchain.presentation.complexcomponents.QuickFillDisplayAndAmount
import com.blockchain.transactions.common.CombinedSourceNetworkFees
import com.blockchain.transactions.common.CryptoAccountWithBalance
import com.blockchain.transactions.common.OnChainDepositEngineInteractor
import com.blockchain.transactions.common.OnChainDepositInputValidationError
import com.blockchain.transactions.sell.SellAnalyticsEvents
import com.blockchain.transactions.sell.SellService
import com.blockchain.utils.removeLeadingZeros
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.CurrencyType
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.balance.isLayer2Token
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.roundToInt
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SellEnterAmountViewModel(
    private val args: SellEnterAmountArgs,
    private val analytics: Analytics,
    private val sellService: SellService,
    private val tradeDataService: TradeDataService,
    private val assetCatalogue: AssetCatalogue,
    private val onChainDepositEngineInteractor: OnChainDepositEngineInteractor,
) : MviViewModel<
    SellEnterAmountIntent,
    SellEnterAmountViewState,
    SellEnterAmountModelState,
    SellEnterAmountNavigationEvent,
    ModelConfigArgs.NoArgs
    >(
    SellEnterAmountModelState()
) {

    private var configJob: Job? = null
    private val fiatInputChanges = MutableSharedFlow<FiatValue>()
    private val cryptoInputChanges = MutableSharedFlow<CryptoValue>()

    init {
        viewModelScope.launch {
            val bestToAccount = sellService.bestTargetAccount()

            // if no account found -> fatal error loading accounts
            if (bestToAccount == null) {
                updateState {
                    copy(
                        fatalError = SellEnterAmountFatalError.WalletLoading
                    )
                }
                return@launch
            }

            val argsSourceAccount = args.sourceAccount.data
            val (fromAccount, toAccount) = if (argsSourceAccount != null) {
                /* If the user already selected a source account we'll try to get the target fiat account to be the user's
                   selected trading currency, if it's not possible we'll select the best possible candidate */

                val balance = argsSourceAccount.balance().firstOrNull()
                val fromAccount = balance?.let {
                    CryptoAccountWithBalance(
                        account = argsSourceAccount,
                        balanceCrypto = balance.total as CryptoValue,
                        balanceFiat = balance.totalFiat as FiatValue,
                    )
                }

                if (balance == null) {
                    // if no account balance -> fatal error loading accounts
                    null to null
                } else if (sellService.isTargetAccountValidForSource(argsSourceAccount, bestToAccount)) {
                    fromAccount to bestToAccount
                } else {
                    val toAccount = sellService.targetAccounts(argsSourceAccount).firstOutcome()
                        .getOrDefault(null)
                        ?.firstOrNull()

                    fromAccount to toAccount
                }
            } else {
                /* Given that the user will be unable to change their target fiat currency in this flow, we first get the
                   their selected trading currency as the target and only then we get the valid source accounts
                   that are available to sell to the fiat currency */

                // get highest balance account as the primary FROM
                val fromAccountWithBalance = sellService.bestSourceAccountForTarget(bestToAccount)

                // if no account found -> fatal error loading accounts

                fromAccountWithBalance to bestToAccount
            }

            if (fromAccount == null || toAccount == null) {
                updateState {
                    copy(
                        fatalError = SellEnterAmountFatalError.WalletLoading
                    )
                }
                return@launch
            }

            analytics.logEvent(SellAnalyticsEvents.EnterAmountViewed(fromAccount.account.currency.networkTicker))

            updateState {
                copy(
                    fromAccount = fromAccount,
                    toAccount = toAccount,
                )
            }

            updateConfig(fromAccount.account, toAccount, null)

            tradeDataService.getQuickFillRoundingForSell()
                .doOnSuccess { data ->
                    updateState { copy(quickFillRoundingData = data) }
                }
                .doOnFailure {
                    // TODO(aromano): SWAP
                }
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

    override fun SellEnterAmountModelState.reduce() = run {
        Logger.e(
            """
            fromAccount: $fromAccount
            toAccount: $toAccount
            isLoadingLimits: $isLoadingLimits
            productLimits: $productLimits
            quickFillRoundingData: $quickFillRoundingData
            sourceToTargetExchangeRate: $sourceToTargetExchangeRate
            sourceNetworkFees: $sourceNetworkFees
            depositEngineInputValidationError: $depositEngineInputValidationError
            fiatAmount: $fiatAmount
            fiatAmountUserInput: $fiatAmountUserInput
            cryptoAmount: $cryptoAmount
            cryptoAmountUserInput: $cryptoAmountUserInput
            selectedInput: $selectedInput
            inputError: ${validateAmount()}
            fatalError: $fatalError
            minLimit: ${try {minLimit} catch (ex: Exception) {ex}}
            maxLimit: ${try {maxLimit} catch (ex: Exception) {ex}}
            """.trimIndent()
        )
        val isInputAmountZero = when (selectedInput) {
            CurrencyType.CRYPTO -> cryptoAmount == null || cryptoAmount.isZero
            CurrencyType.FIAT -> fiatAmount == null || fiatAmount.isZero
        }
        val shouldValidateInput = !isInputAmountZero && sourceToTargetExchangeRate != null
        val inputError = if (shouldValidateInput) {
            validateAmount()
        } else {
            null
        }
        SellEnterAmountViewState(
            selectedInput = when (selectedInput) {
                CurrencyType.FIAT -> InputCurrency.Currency1
                CurrencyType.CRYPTO -> InputCurrency.Currency2
            },
            assets = safeLet(fromAccount, toAccount) { fromAccount, toAccount ->
                EnterAmountAssets(
                    from = fromAccount.account.toViewState(),
                    to = toAccount.toViewState()
                )
            },
            quickFillButtonData = safeLet(
                spendableBalance,
                quickFillRoundingData,
                minLimit,
                maxLimit,
            ) { spendableBalance, quickFillRoundingData, minLimit, maxLimit ->
                val limits = if (minLimit > maxLimit) maxLimit..maxLimit else minLimit..maxLimit
                val cryptoData =
                    getQuickFillCryptoButtonData(spendableBalance, limits, quickFillRoundingData)
                when (selectedInput) {
                    CurrencyType.CRYPTO -> cryptoData
                    CurrencyType.FIAT -> sourceToTargetExchangeRate?.let { rate ->
                        cryptoData.toFiat(rate, limits)
                    }
                }
            },
            fiatAmount = toAccount?.let {
                CurrencyValue(
                    value = when (selectedInput) {
                        CurrencyType.FIAT -> fiatAmountUserInput
                        CurrencyType.CRYPTO -> fiatAmount?.toStringWithoutSymbol().orEmpty().ifEmpty { "0" }
                    },
                    maxFractionDigits = fiatAmount?.userDecimalPlaces ?: 2,
                    ticker = toAccount.currency.symbol,
                    isPrefix = true,
                    separateWithSpace = false,
                    zeroHint = "0"
                )
            },
            cryptoAmount = fromAccount?.let {
                CurrencyValue(
                    value = when (selectedInput) {
                        CurrencyType.CRYPTO -> cryptoAmountUserInput
                        CurrencyType.FIAT -> cryptoAmount?.toStringWithoutSymbol().orEmpty().ifEmpty { "0" }
                    },
                    maxFractionDigits = cryptoAmount?.userDecimalPlaces ?: 8,
                    ticker = fromAccount.account.currency.displayTicker,
                    isPrefix = false,
                    separateWithSpace = true,
                    zeroHint = "0"
                )
            },
            previewButtonState = when {
                !shouldValidateInput -> PreviewButtonState.Disabled
                inputError != null -> PreviewButtonState.Error(inputError)
                else -> PreviewButtonState.Enabled
            },
            // If there's an input error, most likely the quote/price endpoint or the deposit engine will fail in some way
            // therefore we're giving preference to showing the inputError rather than the snackbarError
            snackbarError = (getDepositNetworkFeeError ?: getQuotePriceError).takeIf { inputError == null },
            fatalError = fatalError,
        )
    }

    override suspend fun handleIntent(modelState: SellEnterAmountModelState, intent: SellEnterAmountIntent) {
        when (intent) {
            SellEnterAmountIntent.FlipInputs -> {
                updateState {
                    copy(
                        selectedInput = when (selectedInput) {
                            CurrencyType.CRYPTO -> CurrencyType.FIAT
                            CurrencyType.FIAT -> CurrencyType.CRYPTO
                        }
                    )
                }
            }

            is SellEnterAmountIntent.KeyboardClicked -> {
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

            is SellEnterAmountIntent.FromAccountChanged -> {
                val toAccount = modelState.toAccount
                // verify if current TO account is still valid
                val isToAccountStillValid = toAccount?.let {
                    sellService.isTargetAccountValidForSource(
                        sourceAccount = intent.account.account,
                        targetAccount = it
                    )
                } ?: false

                if (isToAccountStillValid) {
                    updateState {
                        copy(
                            fromAccount = intent.account,
                            secondPassword = intent.secondPassword,
                            toAccount = toAccount,
                            productLimits = null,
                            fiatAmount = null,
                            fiatAmountUserInput = "",
                            cryptoAmount = null,
                            cryptoAmountUserInput = "",
                            sourceToTargetExchangeRate = null,
                            sourceNetworkFees = null,
                            fatalError = null,
                        )
                    }

                    updateConfig(intent.account.account, toAccount, null)
                } else {
                    navigate(SellEnterAmountNavigationEvent.TargetAssets(intent.account, intent.secondPassword))
                }
            }

            is SellEnterAmountIntent.FromAndToAccountsChanged -> {
                updateState {
                    copy(
                        fromAccount = intent.fromAccount,
                        secondPassword = intent.secondPassword,
                        toAccount = intent.toAccount,
                        productLimits = null,
                        fiatAmount = null,
                        fiatAmountUserInput = "",
                        cryptoAmount = null,
                        cryptoAmountUserInput = "",
                        sourceToTargetExchangeRate = null,
                        sourceNetworkFees = null,
                        fatalError = null,
                    )
                }

                updateConfig(intent.fromAccount.account, intent.toAccount, modelState.cryptoAmount)
            }

            is SellEnterAmountIntent.QuickFillEntryClicked -> {
                analytics.logEvent(SellAnalyticsEvents.EnterAmountQuickFillClicked(isPartial = true))

                when (modelState.selectedInput) {
                    CurrencyType.FIAT -> fiatInputChanged(intent.entry.amount.toInputString())
                    CurrencyType.CRYPTO -> cryptoInputChanged(intent.entry.amount.toInputString())
                }
            }

            SellEnterAmountIntent.MaxSelected -> {
                analytics.logEvent(SellAnalyticsEvents.EnterAmountQuickFillClicked(isPartial = false))
                val userInputBalance = modelState.maxLimit.toInputString()

                when (modelState.selectedInput) {
                    CurrencyType.FIAT -> {
                        /*
                        Similarly to the problem stated in startQuotePriceRefreshing() because /brokerage/price only takes in crypto
                        to return the resulting sourceToCryptoExchangeRate, and this rate being variable depending on the amount being sold,
                        it will cause the maxLimit in Fiat to keep changing depending on the amount sent. Eg:
                            * user has 100 Crypto balance
                            * user types 10 Fiat
                            * /brokerage/price returns sourceToTargetExchangeRate of 1.0
                            * so maxLimit is now 100 Crypto or 100 Fiat
                            * user clicks max, we input 100 Fiat
                            * /brokerage/price now returns sourceToTargetExchangeRate of 1.1
                            * so maxLimit is now only 90 Fiat
                            * so the current 100 Fiat input is over the max as thus invalid

                        To prevent this, when the user clicks Max we switch the input to CRYPTO and input the crypto maxLimit instead
                        which is stable and doesn't depend on the /brokerage/price rate
                         */
                        updateState { copy(selectedInput = CurrencyType.CRYPTO) }
                        cryptoInputChanged(userInputBalance)
                    }
                    CurrencyType.CRYPTO -> {
                        cryptoInputChanged(userInputBalance)
                    }
                }
            }

            SellEnterAmountIntent.PreviewClicked -> {
                val fromAccount = modelState.fromAccount?.account
                check(fromAccount != null)
                val toAccount = modelState.toAccount
                check(toAccount != null)
                val cryptoAmount = modelState.cryptoAmount
                check(cryptoAmount != null)

                analytics.logEvent(SellAnalyticsEvents.EnterAmountPreviewClicked(modelState.selectedInput))

                navigate(
                    SellEnterAmountNavigationEvent.Preview(
                        sourceAccount = fromAccount,
                        targetAccount = toAccount,
                        sourceCryptoAmount = cryptoAmount,
                        secondPassword = modelState.secondPassword
                    )
                )
            }

            SellEnterAmountIntent.SnackbarErrorHandled -> {
                updateState { copy(getDepositNetworkFeeError = null, getQuotePriceError = null) }
            }
        }
    }

    private suspend fun fiatInputChanged(newInput: String) {
        val toAccount = modelState.toAccount
        val fiatCurrency = toAccount?.currency
        check(fiatCurrency != null)

        val fiatAmount = newInput.takeIf { it.isNotEmpty() }
            ?.let { FiatValue.fromMajor(fiatCurrency, it.toBigDecimal()) }
            ?: FiatValue.zero(fiatCurrency)

        val cryptoAmount: CryptoValue? = modelState.sourceToTargetExchangeRate
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

        val fiatAmount: FiatValue? = modelState.sourceToTargetExchangeRate
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
        targetAccount: FiatAccount,
        amount: CryptoValue,
    ) {
        getSourceNetworkFeeJob?.cancel()
        getSourceNetworkFeeJob = viewModelScope.launch {
            updateState { copy(depositEngineInputValidationError = null, getDepositNetworkFeeError = null) }
            onChainDepositEngineInteractor.getDepositNetworkFee(AssetAction.Sell, sourceAccount, targetAccount, amount)
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
                        AssetAction.Sell,
                        sourceAccount,
                        targetAccount,
                        amount
                    ).doOnFailure { error ->
                        updateState { copy(depositEngineInputValidationError = error) }
                    }
                }
        }
    }

    private var quotePriceRefreshingJob: Job? = null
    private fun startQuotePriceRefreshing(
        sourceAccount: CryptoAccount,
        targetAccount: FiatAccount,
        amount: CryptoValue,
    ) {
        val refreshDelay = 10_000L
        val pair = CurrencyPair(sourceAccount.currency, targetAccount.currency)
        val direction = getTransferDirection(sourceAccount)

        quotePriceRefreshingJob?.cancel()
        quotePriceRefreshingJob = viewModelScope.launch {
            while (true) {
                updateState { copy(getQuotePriceError = null) }
                tradeDataService.getSellQuotePrice(pair, amount, direction)
                    .doOnSuccess { quotePrice ->
                        val rate = quotePrice.sourceToDestinationRate
                        /*
                            There's a problem we need to address when the user is inputting Fiat:
                            The price endpoint only takes in Crypto which means that when the user
                            inputs Fiat we have to convert to crypto using the previous rate and then send that converted crypto
                            to /brokerage/price, the problem arises when the rate that we used to convert to crypto when sending the amount
                            is different than what we get back from the /brokerage/price.
                            Eg. current rate: 1.0
                                * user inputs 10 Fiat
                                * we convert to 10 Crypto
                                * we send 10 Crypto to /brokerage/price which returns us resultAmount: 9 Fiat and rate: 0.9
                                * If the user was inputting Crypto there would be no problem, the Crypto input would remain 10 Crypto
                                    and the resulting Fiat would change to 9 Fiat
                                * However if the user is inputting Fiat as in this example we simply cannot change it's input to 9
                                    because the user is actively interacting with it and the user meant to actually sell 9 Fiat worth of Crypto
                                * So what we need to do is convert the 10 Fiat into Crypto using the new Rate and update the Crypto being shown
                                * So in case the user would still see 10 Fiat in the input
                                * We convert 10 Fiat into Crypto using the new 0.9 Rate, which would be around 11 Crypto
                                * We update the cryptoAmount to show the new crypto amount
                                * *WARNING* So at this point Crypto has changed, and thus we should go again to the /brokerage/price endpoint
                                    to get the most up to date rate for this amount, but this process would be never ending because we'd get a slightly
                                    different rate back, update the crypto and call /brokerage/price again, etc...
                                    So we don't go to /brokerage/price again, we assume the rate is correct until the next /brokerage/price refresh cycle
                         */
                        when (modelState.selectedInput) {
                            CurrencyType.CRYPTO -> {
                                // The cryptoToFiat rate might have changed so we have to keep the cryptoInput as it
                                // is but we need to update the resulting fiatAmount
                                val cryptoAmount = modelState.cryptoAmount ?: CryptoValue.zero(sourceAccount.currency)
                                val newFiatAmount = rate.convert(cryptoAmount) as FiatValue
                                updateState {
                                    copy(
                                        sourceToTargetExchangeRate = rate,
                                        fiatAmountUserInput = newFiatAmount.toInputString(),
                                        fiatAmount = newFiatAmount,
                                    )
                                }
                            }
                            CurrencyType.FIAT -> {
                                val fiatAmount = modelState.fiatAmount ?: FiatValue.zero(targetAccount.currency)
                                val newCryptoAmount = rate.inverse().convert(fiatAmount) as CryptoValue
                                updateState {
                                    copy(
                                        sourceToTargetExchangeRate = rate,
                                        cryptoAmountUserInput = newCryptoAmount.toInputString(),
                                        cryptoAmount = newCryptoAmount,
                                    )
                                }
                            }
                        }
                    }
                    .doOnFailure { error ->
                        updateState { copy(getQuotePriceError = error) }
                    }

                delay(refreshDelay)
            }
        }
    }

    private fun updateConfig(fromAccount: CryptoAccount, toAccount: FiatAccount?, cryptoAmount: CryptoValue?) {
        configJob?.cancel()
        getSourceNetworkFeeJob?.cancel()

        if (toAccount == null) return
        val amount = cryptoAmount ?: CryptoValue.zero(fromAccount.currency)

        startQuotePriceRefreshing(fromAccount, toAccount, amount)

        if (fromAccount is CryptoNonCustodialAccount) {
            getSourceNetworkFeeAndDepositEngineInputValidation(fromAccount, toAccount, amount)
        } else {
            updateState { copy(sourceNetworkFees = CombinedSourceNetworkFees.zero(fromAccount.currency)) }
        }

        configJob = viewModelScope.launch {
            updateState { copy(isLoadingLimits = true) }
            sellService.limits(
                from = fromAccount.currency as CryptoCurrency,
                to = toAccount.currency,
                fiat = toAccount.currency,
                direction = getTransferDirection(fromAccount),
            ).doOnSuccess { limits ->
                updateState { copy(productLimits = limits) }
            }.doOnFailure {
                // TODO(aromano):
                updateState { copy() }
            }
            updateState { copy(isLoadingLimits = false) }
        }
    }

    private fun refreshNetworkFeesAndDepositEngineInputValidation() {
        val fromAccount = modelState.fromAccount?.account ?: return
        val toAccount = modelState.toAccount ?: return
        val cryptoAmount = modelState.cryptoAmount ?: return

        startQuotePriceRefreshing(fromAccount, toAccount, cryptoAmount)

        if (fromAccount is CryptoNonCustodialAccount) {
            getSourceNetworkFeeAndDepositEngineInputValidation(fromAccount, toAccount, cryptoAmount)
        }
    }

    /**
     * update the fiat value based on the crypto value input
     * and update input errors
     */
    private fun SellEnterAmountModelState.validateAmount(): SellEnterAmountInputError? {
        val cryptoAmount = cryptoAmount ?: return null
        val minLimit = minLimit
        val maxLimit = maxLimit
        val spendableBalance = spendableBalance

        val spendableBalanceFiat = safeLet(
            sourceToTargetExchangeRate,
            spendableBalance,
        ) { rate, limit ->
            rate.convert(limit, RoundingMode.CEILING)
        }
        val minLimitFiat = safeLet(
            sourceToTargetExchangeRate,
            minLimit,
        ) { rate, limit ->
            rate.convert(limit, RoundingMode.CEILING)
        }
        val maxLimitFiat = safeLet(
            sourceToTargetExchangeRate,
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
                SellEnterAmountInputError.AboveBalance(
                    displayTicker = spendableBalance.currency.displayTicker,
                    balance = spendableBalanceString,
                )
            }

            minLimit != null && cryptoAmount < minLimit -> {
                SellEnterAmountInputError.BelowMinimum(minValue = minLimitString)
            }

            maxLimit != null && cryptoAmount > maxLimit -> {
                SellEnterAmountInputError.AboveMaximum(maxValue = maxLimitString)
            }

            else -> when (val error = depositEngineInputValidationError) {
                OnChainDepositInputValidationError.InsufficientFunds -> SellEnterAmountInputError.AboveBalance(
                    displayTicker = spendableBalance?.currency?.displayTicker ?: "-",
                    balance = spendableBalanceString,
                )
                OnChainDepositInputValidationError.InsufficientGas -> {
                    val asset = (sourceNetworkFees?.feeForAmount ?: spendableBalance)?.currency
                    SellEnterAmountInputError.InsufficientGas(
                        networkName = asset?.name ?: "-",
                        displayTicker = asset?.displayTicker ?: "-"
                    )
                } is OnChainDepositInputValidationError.Unknown -> SellEnterAmountInputError.Unknown(error.error)
                null -> null
            }
        }
    }

    private fun getTransferDirection(sourceAccount: CryptoAccount): TransferDirection =
        when (sourceAccount) {
            is NonCustodialAccount -> TransferDirection.FROM_USERKEY
            else -> TransferDirection.INTERNAL
        }

    private fun SingleAccount.toViewState() = EnterAmountAssetState(
        iconUrl = currency.logo,
        nativeAssetIconUrl = (this as? CryptoNonCustodialAccount)?.currency
            ?.takeIf { it.isLayer2Token }
            ?.coinNetwork?.nativeAssetTicker
            ?.let { assetCatalogue.fromNetworkTicker(it)?.logo },
        ticker = currency.displayTicker,
        name = currency.name,
    )

    private fun validateInput(input: String, maxFractionDigits: Int): Boolean {
        val pattern = let {
            val decimalSeparator = DecimalFormatSymbols(Locale.getDefault()).decimalSeparator.toString()
            Pattern.compile(
                "-?\\d{0,100}+((\\$decimalSeparator\\d{0,$maxFractionDigits})?)||(\\$decimalSeparator)?"
            )
        }

        return pattern.matcher(input).matches()
    }

    companion object {
        private const val VALIDATION_DEBOUNCE_MS = 400L
    }
}

private val SellEnterAmountModelState.currencyAwareMaxAmount: Money?
    get() = when (selectedInput) {
        CurrencyType.FIAT -> safeLet(
            sourceToTargetExchangeRate,
            maxLimit,
        ) { rate, limit ->
            rate.convert(limit, RoundingMode.FLOOR)
        }
        CurrencyType.CRYPTO -> maxLimit
    }

private fun Money?.toInputString(): String = this?.toBigDecimal()
    ?.setScale(this.userDecimalPlaces, RoundingMode.FLOOR)
    ?.stripTrailingZeros()
    ?.takeIf { it != BigDecimal.ZERO }
    ?.toPlainString()
    .orEmpty()

private fun getQuickFillCryptoButtonData(
    spendableBalance: CryptoValue,
    limits: ClosedRange<CryptoValue>,
    roundingData: List<QuickFillRoundingData.SellSwapRoundingData>,
): QuickFillButtonData {
    val spendableBalanceWithinLimits = spendableBalance.coerceIn(limits)

    val quickFillEntries = roundingData.mapIndexedNotNull { index, data ->
        val prefillAmount = spendableBalanceWithinLimits.times(data.multiplier) as CryptoValue

        if (prefillAmount in limits) {
            QuickFillDisplayAndAmount(
                displayValue = "${(data.multiplier * 100).toInt()}%",
                amount = prefillAmount,
                position = index,
                roundingData = data,
            )
        } else {
            null
        }
    }

    return QuickFillButtonData(
        maxAmount = spendableBalanceWithinLimits.takeIf { it.isPositive },
        quickFillButtons = quickFillEntries.distinct()
    )
}

private fun QuickFillButtonData.toFiat(
    cryptoToFiatExchangeRate: ExchangeRate,
    limits: ClosedRange<CryptoValue>,
): QuickFillButtonData {
    return QuickFillButtonData(
        maxAmount = maxAmount?.let { cryptoToFiatExchangeRate.convert(it) },
        quickFillButtons = quickFillButtons.mapNotNull { data ->
            val (prefillFiat, prefillCrypto) = getRoundedFiatAndCryptoValues(
                data,
                cryptoToFiatExchangeRate,
            )

            if (prefillCrypto in limits) {
                data.copy(
                    displayValue = prefillFiat.toStringWithSymbol(includeDecimalsWhenWhole = false),
                    amount = prefillFiat,
                )
            } else {
                null
            }
        }
    )
}

private fun getRoundedFiatAndCryptoValues(
    data: QuickFillDisplayAndAmount,
    cryptoToFiatExchangeRate: ExchangeRate,
): Pair<FiatValue, CryptoValue> {
    val roundingValues = (data.roundingData as QuickFillRoundingData.SellSwapRoundingData).rounding
    require(roundingValues.size == 6) { "rounding values missing" }

    val prefillFiat = cryptoToFiatExchangeRate.convert(data.amount)
    val prefillFiatParts = prefillFiat.toStringParts()

    val roundToNearest: (lastAmount: Money, nearest: Int) -> Money = { lastAmount, nearest ->
        Money.fromMajor(
            lastAmount.currency,
            (nearest * ((lastAmount.toFloat() / nearest).roundToInt())).toBigDecimal()
        )
    }

    val lowestPrefillRoundedFiat = when (
        prefillFiatParts.major.filterNot {
            it == prefillFiatParts.groupingSeparator
        }.length
    ) {
        0,
        1 -> {
            roundToNearest(prefillFiat, roundingValues[0])
        }
        2 -> {
            roundToNearest(prefillFiat, roundingValues[1])
        }
        3 -> {
            roundToNearest(prefillFiat, roundingValues[2])
        }
        4 -> {
            roundToNearest(prefillFiat, roundingValues[3])
        }
        5 -> {
            roundToNearest(prefillFiat, roundingValues[4])
        }
        else -> {
            roundToNearest(prefillFiat, roundingValues[5])
        }
    } as FiatValue

    val lowestPrefillRoundedCrypto =
        cryptoToFiatExchangeRate.inverse().convert(lowestPrefillRoundedFiat) as CryptoValue

    return lowestPrefillRoundedFiat to lowestPrefillRoundedCrypto
}
