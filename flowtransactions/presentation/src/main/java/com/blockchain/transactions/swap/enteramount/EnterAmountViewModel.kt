package com.blockchain.transactions.swap.enteramount

import androidx.lifecycle.viewModelScope
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
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.extensions.safeLet
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.flatMapData
import com.blockchain.store.mapData
import com.blockchain.transactions.swap.CryptoAccountWithBalance
import com.blockchain.transactions.swap.SwapService
import com.blockchain.transactions.swap.confirmation.composable.ConfirmationArgs
import com.blockchain.utils.removeLeadingZeros
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import java.math.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * @property fromTicker if we come from Coinview we should have preset FROM
 */
@OptIn(FlowPreview::class)
class EnterAmountViewModel(
    private val fromTicker: String? = null,
    private val swapService: SwapService,
    private val exchangeRates: ExchangeRatesDataManager,
    private val currencyPrefs: CurrencyPrefs
) : MviViewModel<
    EnterAmountIntent,
    EnterAmountViewState,
    EnterAmountModelState,
    EnterAmountNavigationEvent,
    ModelConfigArgs.NoArgs
    >(
    EnterAmountModelState()
) {

    private val fromAccountFlow = MutableSharedFlow<CryptoAccountWithBalance>(replay = 1)
    private val toAccountTickerFlow = MutableSharedFlow<String>(replay = 1) // accept cryptoaccounts instead of tickers

    private val fiatInputChanges = MutableSharedFlow<String>()
    private val cryptoInputChanges = MutableSharedFlow<String>()

    init {
        loadAccountsAndConfig()

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
            assets = accounts.map {
                EnterAmountAssets(
                    from = it.fromAccount.account.currency.toViewState(),
                    to = it.toAccount?.currency?.toViewState()
                )
            },
            accountBalance = state.accounts.map {
                when (state.selectedInput) {
                    InputCurrency.Currency1 -> it.fromAccount.balanceFiat
                    InputCurrency.Currency2 -> it.fromAccount.balanceCrypto
                }.toStringWithSymbol()
            }.dataOrElse(null),
            fiatAmount = accounts.map {
                CurrencyValue(
                    value = if (selectedInput == InputCurrency.Currency1) {
                        fiatAmountUserInput
                    } else {
                        //                            fiatAmountUserInput.ifEmpty { "0" }
                        // format the unfocused value?
                        fiatAmount?.toStringWithoutSymbol().orEmpty().ifEmpty { "0" }
                    },
                    maxFractionDigits = fiatAmount?.userDecimalPlaces ?: 2,
                    ticker = it.fiatCurrency.symbol,
                    isPrefix = true,
                    separateWithSpace = false
                )
            }.dataOrElse(null),
            cryptoAmount = accounts.map {
                CurrencyValue(
                    value = if (selectedInput == InputCurrency.Currency2) {
                        cryptoAmountUserInput
                    } else {
                        //                            cryptoAmountUserInput.ifEmpty { "0" }
                        // format the unfocused value?
                        cryptoAmount?.toStringWithoutSymbol().orEmpty().ifEmpty { "0" }
                    },
                    maxFractionDigits = cryptoAmount?.userDecimalPlaces ?: 8,
                    ticker = it.fromAccount.account.currency.displayTicker,
                    isPrefix = false,
                    separateWithSpace = true
                )
            }.dataOrElse(null),
            error = inputError
        )
    }

    override suspend fun handleIntent(modelState: EnterAmountModelState, intent: EnterAmountIntent) {
        when (intent) {
            EnterAmountIntent.LoadData -> {
                // handle predefined from
                val fromAccount = swapService.highestBalanceSourceAccount()

                val toAccountTicker = when (fromAccount?.account?.currency?.networkTicker) {
                    "BTC" -> "USDT"
                    else -> "BTC"
                }

                safeLet(
                    fromAccount,
                    toAccountTicker
                ) { from, to ->
                    fromAccountFlow.emit(from)
                    toAccountTickerFlow.emit(to)
                } ?: updateState {
                    it.copy(
                        fatalError = SwapEnterAmountFatalError.WalletLoading
                    )
                }
            }

            EnterAmountIntent.FlipInputs -> {
                updateState {
                    it.copy(
                        selectedInput = it.selectedInput.flip()
                    )
                }
            }

            is EnterAmountIntent.FiatInputChanged -> {
                val fiatCurrency = modelState.accounts.map { it.fiatCurrency }.dataOrElse(null)
                check(fiatCurrency != null)

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
                val fromCurrency = modelState.accounts.map { it.fromAccount.account.currency }.dataOrElse(null)
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
                fromAccountFlow.emit(intent.account)
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
                val accounts = (modelState.accounts as DataResource.Data).data
                check(accounts.toAccount != null)
                val data = ConfirmationArgs(
                    sourceAccount = accounts.fromAccount.account,
                    targetAccount = accounts.toAccount,
                    sourceCryptoAmount = modelState.cryptoAmount!!,
                    direction = TransferDirection.INTERNAL, // TODO(aromano): TEMP
                    secondPassword = null, // TODO(aromano): TEMP
                )
                navigate(EnterAmountNavigationEvent.Preview(data))
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadAccountsAndConfig() {
        viewModelScope.launch {
            fromAccountFlow
                .map { DataResource.Data(it) }
                .flatMapData { fromAccount ->
                    toAccountTickerFlow.flatMapLatest { toTicker ->
                        swapService.targetAccounts(fromAccount.account)
                            .mapData {
                                it.firstOrNull { it.currency.networkTicker == toTicker }
                            }
                    }.map {
                        it.map { toAccount ->
                            EnterAmountAccounts(
                                fromAccount = fromAccount,
                                toAccount = toAccount,
                                fiatCurrency = currencyPrefs.selectedFiatCurrency,
                            )
                        }
                    }
                }
                .onEach { accountsData ->
                    // reset all whenever a new asset is selected
                    updateState {
                        it.copy(
                            accounts = it.accounts.updateDataWith(accountsData),
                            fiatAmount = null,
                            fiatAmountUserInput = "",
                            cryptoAmount = null,
                            cryptoAmountUserInput = "",
                            inputError = null,
                            fatalError = null
                        )
                    }
                }.flatMapData { accounts ->
                    if (accounts.toAccount == null) {
                        updateState {
                            it.copy(config = DataResource.Loading)
                        }
                        return@flatMapData flowOf(DataResource.Loading)
                    }

                    combine(
                        exchangeRates.exchangeRateToUserFiatFlow(accounts.fromAccount.account.currency),
                        swapService.limits(
                            from = accounts.fromAccount.account.currency as CryptoCurrency,
                            to = accounts.toAccount.currency as CryptoCurrency,
                            fiat = accounts.fiatCurrency
                        )
                    ) { exchangeRate, limits ->
                        combineDataResources(
                            exchangeRate,
                            limits
                        ) { exchangeRateData, limitsData ->
                            EnterAmountConfig(
                                sourceAccountToFiat = exchangeRateData,
                                limits = limitsData
                            )
                        }
                    }
                }.onEach { configData ->
                    updateState {
                        it.copy(
                            config = it.config.updateDataWith(configData)
                        )
                    }
                }.collect()
        }
    }

    /**
     * update the crypto value based on the fiat value input
     * and update input errors
     */
    private fun onFiatInputChanged(value: String) {
        val fiatCurrency = modelState.accounts.map { it.fiatCurrency }.dataOrElse(null)
        check(fiatCurrency != null)

        val a: CryptoValue? = modelState.config.map { it.sourceAccountToFiat }.dataOrElse(null)?.inverse()?.convert(
            FiatValue.fromMajor(
                fiatCurrency,
                value.ifEmpty { "0" }.toBigDecimal()
            )
        ) as CryptoValue?
        updateState {
            it.copy(
                cryptoAmountUserInput = a?.toBigDecimal()?.stripTrailingZeros()
                    ?.takeIf { it != BigDecimal.ZERO }
                    ?.toString().orEmpty(),
                cryptoAmount = a
            )
        }

        // check errors
        safeLet(
            modelState.fiatAmount,
            modelState.config.map { it.limits }.dataOrElse(null)?.min?.amount,
            modelState.config.map { it.limits }.dataOrElse(null)?.max,
            modelState.config.map { it.sourceAccountToFiat }.dataOrElse(null)
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

                        modelState.accounts.map { fiatAmount > it.fromAccount.balanceFiat }
                            .dataOrElse(false) -> {
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
        val fromCurrency = modelState.accounts.map { it.fromAccount.account.currency }.dataOrElse(null)
        check(fromCurrency != null)

        val a: FiatValue? = modelState.config.map { it.sourceAccountToFiat }.dataOrElse(null)?.convert(
            CryptoValue.fromMajor(
                fromCurrency,
                value.ifEmpty { "0" }.toBigDecimal()
            )
        ) as FiatValue?

        updateState {
            it.copy(
                fiatAmountUserInput = a?.toBigDecimal()?.stripTrailingZeros()
                    ?.takeIf { it != BigDecimal.ZERO }
                    ?.toString().orEmpty(),
                fiatAmount = a
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

                        modelState.accounts.map { cryptoAmount > it.fromAccount.balanceCrypto }
                            .dataOrElse(false) -> {
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
    get() = accounts.map {
        when (selectedInput) {
            InputCurrency.Currency1 -> it.fromAccount.balanceFiat
            InputCurrency.Currency2 -> it.fromAccount.balanceCrypto
        }
    }.dataOrElse(null)
