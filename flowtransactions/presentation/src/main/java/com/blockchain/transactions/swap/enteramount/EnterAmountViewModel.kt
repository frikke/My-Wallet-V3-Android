package com.blockchain.transactions.swap.enteramount

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.componentlib.control.CurrencyValue
import com.blockchain.componentlib.control.InputCurrency
import com.blockchain.componentlib.control.flip
import com.blockchain.core.limits.TxLimit
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResources
import com.blockchain.data.dataOrElse
import com.blockchain.data.map
import com.blockchain.data.updateDataWith
import com.blockchain.extensions.safeLet
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.transactions.swap.SwapService
import com.blockchain.utils.removeLeadingZeros
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import java.math.BigDecimal
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
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
    currencyPrefs: CurrencyPrefs
) : MviViewModel<
    EnterAmountIntent,
    EnterAmountViewState,
    EnterAmountModelState,
    EnterAmountNavigationEvent,
    ModelConfigArgs.NoArgs
    >(
    EnterAmountModelState(fiatCurrency = currencyPrefs.selectedFiatCurrency)
) {

    init {
        viewModelScope.launch {
            val walletMode = walletModeService.walletMode.firstOrNull()
            updateState {
                it.copy(
                    walletMode = walletMode,
                )
            }
        }
    }

    private var configJob: Job? = null
    private val fiatInputChanges = MutableSharedFlow<String>()
    private val cryptoInputChanges = MutableSharedFlow<String>()

    init {
        viewModelScope.launch {
            fiatInputChanges.debounce(DEBOUNCE_MS)
                .collectLatest {
                    onFiatInputChanged(it)
                }
        }

        viewModelScope.launch {
            cryptoInputChanges.debounce(DEBOUNCE_MS)
                .collectLatest {
                    onCryptoInputChanged(it)
                }
        }
    }

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: EnterAmountModelState) = state.run {
        EnterAmountViewState(
            selectedInput = selectedInput,
            assets = fromAccount?.let {
                EnterAmountAssets(
                    from = fromAccount.account.currency.toViewState(),
                    to = toAccount?.currency?.toViewState()
                )
            },
            accountBalance = fromAccount?.let {
                when (state.selectedInput) {
                    InputCurrency.Currency1 -> fromAccount.balanceFiat
                    InputCurrency.Currency2 -> fromAccount.balanceCrypto
                }.toStringWithSymbol()
            },
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
            inputError = inputError,
            fatalError = fatalError
        )
    }

    override suspend fun handleIntent(modelState: EnterAmountModelState, intent: EnterAmountIntent) {
        when (intent) {
            EnterAmountIntent.LoadData -> {
                check(modelState.walletMode != null)

                // get highest balance account as the primary FROM
                val fromAccount = swapService.highestBalanceSourceAccount()

                // if no account found -> fatal error loading accounts
                if (fromAccount == null) {
                    updateState {
                        it.copy(
                            fatalError = SwapEnterAmountFatalError.WalletLoading
                        )
                    }
                    return
                }

                val fromAccountTicker = fromAccount.account.currency.networkTicker
                val toAccount = swapService.bestTargetAccountForMode(
                    sourceTicker = fromAccountTicker,
                    targetTicker = initialTargetAccountRule(fromAccountTicker),
                    mode = modelState.walletMode
                )

                updateState {
                    it.copy(
                        fromAccount = fromAccount,
                        toAccount = toAccount,
                    )
                }

                updateConfig(fromAccount.account, toAccount)
            }

            EnterAmountIntent.FlipInputs -> {
                updateState {
                    it.copy(
                        selectedInput = it.selectedInput.flip()
                    )
                }
            }

            is EnterAmountIntent.FiatInputChanged -> {
                val fiatCurrency = modelState.fiatCurrency

                updateState {
                    it.copy(
                        fiatAmountUserInput = intent.amount.removeLeadingZeros(),
                        fiatAmount = intent.amount.takeIf { it.isNotEmpty() }
                            ?.let { FiatValue.fromMajor(fiatCurrency, it.toBigDecimal()) },
                    )
                }

                fiatInputChanges.emit(intent.amount)
            }

            is EnterAmountIntent.CryptoInputChanged -> {
                val fromCurrency = modelState.fromAccount?.account?.currency
                check(fromCurrency != null)

                updateState {
                    it.copy(
                        cryptoAmountUserInput = intent.amount.removeLeadingZeros(),
                        cryptoAmount = intent.amount.takeIf { it.isNotEmpty() }
                            ?.let { CryptoValue.fromMajor(fromCurrency, it.toBigDecimal()) },
                    )
                }

                cryptoInputChanges.emit(intent.amount)
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
                    it.copy(
                        fromAccount = intent.account,
                        toAccount = toAccount,
                        fiatAmountUserInput = "",
                        cryptoAmount = null,
                        cryptoAmountUserInput = "",
                        inputError = null,
                        fatalError = null
                    )
                }

                updateConfig(intent.account.account, toAccount)
            }

            is EnterAmountIntent.ToAccountChanged -> {
                updateState {
                    it.copy(
                        toAccount = intent.account,
                    )
                }

                // update limits
            }

            EnterAmountIntent.MaxSelected -> {
                val userInputBalance = modelState.currencyAwareBalance?.toBigDecimal()?.stripTrailingZeros()
                    ?.takeIf { it != BigDecimal.ZERO }
                    ?.toString().orEmpty()

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

                val data = ConfirmationArgs(
                    sourceAccount = fromAccount,
                    targetAccount = toAccount,
                    sourceCryptoAmount = cryptoAmount,
                    secondPassword = null, // TODO(aromano): TEMP
                )
                navigate(EnterAmountNavigationEvent.Preview(data))
            }
        }
    }

    private fun updateConfig(fromAccount: CryptoAccount, toAccount: CryptoAccount?) {
        configJob?.cancel()

        if (toAccount == null) {
            updateState {
                it.copy(config = DataResource.Loading)
            }
            return
        }

        configJob = viewModelScope.launch {
            combine(
                exchangeRates.exchangeRateToUserFiatFlow(fromAccount.currency),
                swapService.limits(
                    from = fromAccount.currency as CryptoCurrency,
                    to = toAccount.currency as CryptoCurrency,
                    fiat = modelState.fiatCurrency
                )
            ) { exchangeRate, limits ->
                combineDataResources(
                    exchangeRate,
                    limits
                ) { exchangeRateData, limitsData ->
                    EnterAmountConfig(
                        sourceAccountToFiatRate = exchangeRateData,
                        limits = limitsData
                    )
                }
            }.collectLatest { configData ->
                updateState {
                    it.copy(
                        config = it.config.updateDataWith(configData)
                    )
                }
            }
        }
    }

    //    @OptIn(ExperimentalCoroutinesApi::class)
    //    private fun loadAccountsAndConfig() {
    //        viewModelScope.launch {
    //            fromAccountFlow
    //                .map { DataResource.Data(it) }
    //                .flatMapData { fromAccount ->
    //                    toAccountFlow.flatMapLatest { toTicker ->
    //                        swapService.bestTargetAccountForMode(
    //                            sourceTicker = fromAccount.account.currency.networkTicker,
    //                            targetTicker = toTicker,
    //                            WalletMode.CUSTODIAL
    //                        )
    //                    }.map {
    //                        it.map { toAccount ->
    //                            EnterAmountAccounts(
    //                                fromAccount = fromAccount,
    //                                toAccount = toAccount,
    //                                fiatCurrency = currencyPrefs.selectedFiatCurrency,
    //                            )
    //                        }
    //                    }
    //                }
    //                .onEach { accountsData ->
    //                    // reset all whenever a new asset is selected
    //                    updateState {
    //                        it.copy(
    //                            accounts = it.accounts.updateDataWith(accountsData),
    //                            fiatAmount = null,
    //                            fiatAmountUserInput = "",
    //                            cryptoAmount = null,
    //                            cryptoAmountUserInput = "",
    //                            inputError = null,
    //                            fatalError = null
    //                        )
    //                    }
    //                }.flatMapData { accounts ->
    //                    if (accounts.toAccount == null) {
    //                        updateState {
    //                            it.copy(config = DataResource.Loading)
    //                        }
    //                        return@flatMapData flowOf(DataResource.Loading)
    //                    }
    //
    //                    combine(
    //                        exchangeRates.exchangeRateToUserFiatFlow(accounts.fromAccount.account.currency),
    //                        swapService.limits(
    //                            from = accounts.fromAccount.account.currency as CryptoCurrency,
    //                            to = accounts.toAccount.currency as CryptoCurrency,
    //                            fiat = accounts.fiatCurrency
    //                        )
    //                    ) { exchangeRate, limits ->
    //                        combineDataResources(
    //                            exchangeRate,
    //                            limits
    //                        ) { exchangeRateData, limitsData ->
    //                            EnterAmountConfig(
    //                                sourceAccountToFiat = exchangeRateData,
    //                                limits = limitsData
    //                            )
    //                        }
    //                    }
    //                }.onEach { configData ->
    //                    updateState {
    //                        it.copy(
    //                            config = it.config.updateDataWith(configData)
    //                        )
    //                    }
    //                }.collect()
    //        }
    //    }

    /**
     * update the crypto value based on the fiat value input
     * and update input errors
     */
    private fun onFiatInputChanged(value: String) {
        val fiatCurrency = modelState.fiatCurrency

        val cryptoAmount: CryptoValue? = modelState.config.map { it.sourceAccountToFiatRate }.dataOrElse(null)
            ?.inverse()
            ?.convert(
                FiatValue.fromMajor(
                    fiatCurrency,
                    value.ifEmpty { "0" }.toBigDecimal()
                )
            ) as CryptoValue?

        updateState {
            it.copy(
                cryptoAmountUserInput = cryptoAmount?.toBigDecimal()?.stripTrailingZeros()
                    ?.takeIf { it != BigDecimal.ZERO }
                    ?.toString().orEmpty(),
                cryptoAmount = cryptoAmount
            )
        }

        // check errors
        safeLet(
            modelState.fiatAmount,
            modelState.config.map { it.limits }.dataOrElse(null)?.min?.amount,
            modelState.config.map { it.limits }.dataOrElse(null)?.max,
            modelState.config.map { it.sourceAccountToFiatRate }.dataOrElse(null)
        ) { fiatAmount, minAmount, maxAmount, exchangeRate ->
            val minFiatAmount = exchangeRate.convert(minAmount)

            updateState {
                it.copy(
                    inputError = when {
                        fiatAmount < minFiatAmount -> {
                            SwapEnterAmountInputError.BelowMinimum(minValue = minFiatAmount.toStringWithSymbol())
                        }

                        maxAmount is TxLimit.Limited && fiatAmount > exchangeRate.convert(maxAmount.amount) -> {
                            SwapEnterAmountInputError.AboveMaximum(
                                maxValue = exchangeRate.convert(maxAmount.amount).toStringWithSymbol()
                            )
                        }

                        (modelState.fromAccount?.balanceFiat?.let { fiatAmount > it } ?: false) -> {
                            SwapEnterAmountInputError.AboveBalance
                        }

                        else -> {
                            null
                        }
                    }
                )
            }
        } ?: updateState { it.copy(inputError = null) }
    }

    /**
     * update the fiat value based on the crypto value input
     * and update input errors
     */
    private fun onCryptoInputChanged(value: String) {
        val fromCurrency = modelState.fromAccount?.account?.currency
        check(fromCurrency != null)

        val fiatAmount: FiatValue? = modelState.config.map { it.sourceAccountToFiatRate }.dataOrElse(null)?.convert(
            CryptoValue.fromMajor(
                fromCurrency,
                value.ifEmpty { "0" }.toBigDecimal()
            )
        ) as FiatValue?

        updateState {
            it.copy(
                fiatAmountUserInput = fiatAmount?.toBigDecimal()?.stripTrailingZeros()
                    ?.takeIf { it != BigDecimal.ZERO }
                    ?.toString().orEmpty(),
                fiatAmount = fiatAmount
            )
        }

        // check errors
        safeLet(
            modelState.cryptoAmount,
            modelState.config.map { it.limits }.dataOrElse(null)?.min?.amount,
            modelState.config.map { it.limits }.dataOrElse(null)?.max,
        ) { cryptoAmount, minAmount, maxAmount ->
            updateState {
                it.copy(
                    inputError = when {
                        cryptoAmount < minAmount -> {
                            SwapEnterAmountInputError.BelowMinimum(minValue = minAmount.toStringWithSymbol())
                        }

                        maxAmount is TxLimit.Limited && cryptoAmount > maxAmount.amount -> {
                            SwapEnterAmountInputError.AboveMaximum(maxValue = maxAmount.amount.toStringWithSymbol())
                        }

                        (modelState.fromAccount?.balanceCrypto?.let { cryptoAmount > it } ?: false) -> {
                            SwapEnterAmountInputError.AboveBalance
                        }

                        else -> {
                            null
                        }
                    }
                )
            }
        } ?: updateState { it.copy(inputError = null) }
    }

    companion object {
        private const val DEBOUNCE_MS = 400L
    }
}

private fun Currency.toViewState() = EnterAmountAssetState(
    iconUrl = logo,
    ticker = displayTicker
)

private val EnterAmountModelState.currencyAwareBalance: Money?
    get() = fromAccount?.let {
        when (selectedInput) {
            InputCurrency.Currency1 -> fromAccount.balanceFiat
            InputCurrency.Currency2 -> fromAccount.balanceCrypto
        }
    }

private fun initialTargetAccountRule(fromTicker: String) = when (fromTicker) {
    "BTC" -> "USDT"
    else -> "BTC"
}
