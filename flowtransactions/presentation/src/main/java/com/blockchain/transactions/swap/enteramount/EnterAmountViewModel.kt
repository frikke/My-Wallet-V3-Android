package com.blockchain.transactions.swap.enteramount

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.EmptyNavEvent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.componentlib.control.CurrencyValue
import com.blockchain.componentlib.control.InputCurrency
import com.blockchain.componentlib.control.flip
import com.blockchain.core.limits.TxLimit
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.combineDataResources
import com.blockchain.data.dataOrElse
import com.blockchain.data.map
import com.blockchain.data.updateDataWith
import com.blockchain.extensions.safeLet
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.flatMapData
import com.blockchain.store.mapData
import com.blockchain.transactions.swap.SwapService
import com.blockchain.utils.removeLeadingZeros
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Currency
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
) : MviViewModel<EnterAmountIntent, EnterAmountViewState, EnterAmountModelState, EmptyNavEvent, ModelConfigArgs.NoArgs>(
    EnterAmountModelState()
) {

    private val fromAccountTickerFlow = MutableSharedFlow<String>()
    private val toAccountTickerFlow = MutableSharedFlow<String>()

    private val fiatValueChanges = MutableSharedFlow<String>()
    private val cryptoValueChanges = MutableSharedFlow<String>()

    init {
        loadAccountsAndExchangeRate()

        viewModelScope.launch {
            fiatValueChanges.debounce(DEBOUNCE_MS)
                .collectLatest {
                    onFiatValueChanged(it)
                }
        }

        viewModelScope.launch {
            cryptoValueChanges.debounce(DEBOUNCE_MS)
                .collectLatest {
                    onCryptoValueChanged(it)
                }
        }
    }

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: EnterAmountModelState): EnterAmountViewState {
        return with(state) {
            EnterAmountViewState(
                selectedInput = selectedInput,
                assets = combineDataResources(
                    accounts.map { it.fromAccount.account.currency.toViewState() },
                    accounts.map { it.toAccount.currency.toViewState() }
                ) { from, to ->
                    EnterAmountAssets(
                        from = from,
                        to = to
                    )
                },
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
    }

    override suspend fun handleIntent(modelState: EnterAmountModelState, intent: EnterAmountIntent) {
        when (intent) {
            EnterAmountIntent.LoadData -> {
                val fromAccountTicker =
                    fromTicker ?: swapService.highestBalanceSourceAccount()?.account?.currency?.networkTicker

                val toAccountTicker = when (fromAccountTicker) {
                    "BTC" -> "USDT"
                    else -> "BTC"
                }

                println("------ fromAccountTicker $fromAccountTicker")
                println("------ fromAccountTicker to $toAccountTicker")

                safeLet(
                    fromAccountTicker,
                    toAccountTicker
                ) { from, to ->
                    fromAccountTickerFlow.emit(from)
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

            is EnterAmountIntent.FiatAmountChanged -> {
                val fiatCurrency = modelState.accounts.map { it.fiatCurrency }.dataOrElse(null)
                check(fiatCurrency != null)

                updateState {
                    it.copy(
                        fiatAmountUserInput = intent.amount.removeLeadingZeros(),
                        fiatAmount = intent.amount.takeIf { it.isNotEmpty() }
                            ?.let { Money.fromMajor(fiatCurrency, it.toBigDecimal()) },
                    )
                }

                fiatValueChanges.emit(intent.amount)
            }

            is EnterAmountIntent.CryptoAmountChanged -> {
                val fromCurrency = modelState.accounts.map { it.fromAccount.account.currency }.dataOrElse(null)
                check(fromCurrency != null)

                updateState {
                    it.copy(
                        cryptoAmountUserInput = intent.amount.removeLeadingZeros(),
                        cryptoAmount = intent.amount.takeIf { it.isNotEmpty() }
                            ?.let { Money.fromMajor(fromCurrency, it.toBigDecimal()) },
                    )
                }

                cryptoValueChanges.emit(intent.amount)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadAccountsAndExchangeRate() {
        val fromAccountFlow = fromAccountTickerFlow.flatMapLatest { fromTicker ->
            swapService.custodialSourceAccountsWithBalances()
                .mapData {
                    it.first { it.account.currency.networkTicker == fromTicker }
                }
        }

        val toAccountFlow = toAccountTickerFlow.flatMapLatest { toTicker ->
            swapService.sourceAccounts()
                .mapData {
                    it.first { it.currency.networkTicker == toTicker }
                }
        }

        viewModelScope.launch {
            combine(
                fromAccountFlow,
                toAccountFlow
            ) { fromAccount, toAccount ->
                combineDataResources(
                    fromAccount,
                    toAccount
                ) { fromAccountData, toAccountData ->
                    EnterAmountAccounts(
                        fromAccount = fromAccountData,
                        toAccount = toAccountData,
                        fiatCurrency = currencyPrefs.selectedFiatCurrency,
                    )
                }
            }.onEach { accountsData ->
                updateState {
                    it.copy(
                        accounts = it.accounts.updateDataWith(accountsData),
                        fatalError = null
                    )
                }
            }.flatMapData { accounts ->
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
                            exchangeRate = exchangeRateData,
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
    private fun onFiatValueChanged(value: String) {
        val fiatCurrency = modelState.accounts.map { it.fiatCurrency }.dataOrElse(null)
        check(fiatCurrency != null)

        val a: Money? = modelState.config.map { it.exchangeRate }.dataOrElse(null)?.inverse()?.convert(
            Money.fromMajor(
                fiatCurrency,
                value.ifEmpty { "0" }.toBigDecimal()
            )
        )
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
            modelState.config.map { it.exchangeRate }.dataOrElse(null)
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
    private fun onCryptoValueChanged(value: String) {
        val fromCurrency = modelState.accounts.map { it.fromAccount.account.currency }.dataOrElse(null)
        check(fromCurrency != null)

        val a: Money? = modelState.config.map { it.exchangeRate }.dataOrElse(null)?.convert(
            Money.fromMajor(
                fromCurrency,
                value.ifEmpty { "0" }.toBigDecimal()
            )
        )

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
