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
import com.blockchain.data.DataResource
import com.blockchain.extensions.safeLet
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.transactions.swap.SwapService
import com.blockchain.utils.removeLeadingZeros
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

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

    private val fiatValueChanges = MutableSharedFlow<String>()
    private val cryptoValueChanges = MutableSharedFlow<String>()

    init {
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
                fromAsset = fromAccount?.currency?.toViewState(),
                toAsset = toAccount?.currency?.toViewState(),
                fiatAmount = fiatCurrency?.let {
                    CurrencyValue(
                        value = if (selectedInput == InputCurrency.Currency1) {
                            fiatAmountUserInput
                        } else {
                            //                            fiatAmountUserInput.ifEmpty { "0" }
                            // format the unfocused value?
                            fiatAmount?.toStringWithoutSymbol().orEmpty().ifEmpty { "0" }
                        },
                        maxFractionDigits = fiatAmount?.userDecimalPlaces ?: 2,
                        ticker = it.symbol,
                        isPrefix = true,
                        separateWithSpace = false
                    )
                },
                cryptoAmount = fromAccount?.currency?.let {
                    CurrencyValue(
                        value = if (selectedInput == InputCurrency.Currency2) {
                            cryptoAmountUserInput
                        } else {
                            //                            cryptoAmountUserInput.ifEmpty { "0" }
                            // format the unfocused value?
                            cryptoAmount?.toStringWithoutSymbol().orEmpty().ifEmpty { "0" }
                        },
                        maxFractionDigits = cryptoAmount?.userDecimalPlaces ?: 8,
                        ticker = it.displayTicker,
                        isPrefix = false,
                        separateWithSpace = true
                    )
                },
                error = error
            )
        }
    }

    override suspend fun handleIntent(modelState: EnterAmountModelState, intent: EnterAmountIntent) {
        when (intent) {
            EnterAmountIntent.LoadData -> {
                updateState {
                    it.copy(
                        fiatCurrency = currencyPrefs.selectedFiatCurrency
                    )
                }

                viewModelScope.launch {
                    val accounts = (
                        swapService.sourceAccounts().filter { it is DataResource.Data }
                            .firstOrNull() as? DataResource.Data
                        )?.data

                    accounts?.let {
                        updateState {
                            it.copy(
                                fromAccount = accounts.first { it.currency.networkTicker == "DOGE" },
                                toAccount = accounts.first { it.currency.networkTicker == "ETH" },
                                fiatAmount = Money.fromMajor(currencyPrefs.selectedFiatCurrency, BigDecimal.ZERO),
                                cryptoAmount = Money.fromMajor(
                                    accounts.first { it.currency.networkTicker == "DOGE" }.currency, BigDecimal.ZERO
                                ),
                            )
                        }

                        val exchangeRate = (
                            exchangeRates.exchangeRateToUserFiatFlow(
                                accounts.first { it.currency.networkTicker == "DOGE" }.currency
                            ).filter { it is DataResource.Data }.firstOrNull() as? DataResource.Data
                            )?.data

                        updateState {
                            it.copy(
                                exchangeRate = exchangeRate
                            )
                        }

                        val limits = swapService.limits(
                            from = accounts.first { it.currency.networkTicker == "DOGE" }.currency as CryptoCurrency,
                            to = accounts.first { it.currency.networkTicker == "ETH" }.currency as CryptoCurrency,
                            fiat = currencyPrefs.selectedFiatCurrency
                        )
                        updateState {
                            it.copy(
                                limits = limits
                            )
                        }
                    }
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
                val fiatCurrency = modelState.fiatCurrency
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
                val fromCurrency = modelState.fromAccount?.currency
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

    /**
     * update the crypto value based on the fiat value input
     * and update input errors
     */
    private fun onFiatValueChanged(value: String) {
        val fiatCurrency = modelState.fiatCurrency
        check(fiatCurrency != null)

        val a: Money? = modelState.exchangeRate?.inverse()?.convert(
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
            modelState.limits?.min?.amount,
            modelState.limits?.max,
            modelState.exchangeRate
        ) { fiatAmount, minAmount, maxAmount,exchangeRate ->
            val minFiatAmount = exchangeRate.convert(minAmount)

            updateState {
                it.copy(
                    error = when {
                        fiatAmount < minFiatAmount -> {
                            SwapEnterAmountError.BelowMinimum(minValue = minFiatAmount.toStringWithSymbol())
                        }
                        maxAmount is TxLimit.Limited && fiatAmount > exchangeRate.convert(maxAmount.amount) -> {
                            SwapEnterAmountError.AboveMaximum(maxValue = exchangeRate.convert(maxAmount.amount).toStringWithSymbol())
                        }
                        else -> {
                            null
                        }
                    }
                )
            }
        } ?: updateState { it.copy(error = null) }
    }

    /**
     * update the fiat value based on the crypto value input
     * and update input errors
     */
    private fun onCryptoValueChanged(value: String) {
        val fromCurrency = modelState.fromAccount?.currency
        check(fromCurrency != null)

        val a: Money? = modelState.exchangeRate?.convert(
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
            modelState.limits?.min?.amount,
            modelState.limits?.max,
        ) { cryptoAmount, minAmount, maxAmount ->
            updateState {
                it.copy(
                    error = when {
                        cryptoAmount < minAmount -> {
                            SwapEnterAmountError.BelowMinimum(minValue = minAmount.toStringWithSymbol())
                        }
                        maxAmount is TxLimit.Limited && cryptoAmount > maxAmount.amount -> {
                            SwapEnterAmountError.AboveMaximum(maxValue = maxAmount.amount.toStringWithSymbol())
                        }
                        else -> {
                            null
                        }
                    }
                )
            }
        } ?: updateState { it.copy(error = null) }
    }

    companion object {
        private const val DEBOUNCE_MS = 400L
    }
}

private fun Currency.toViewState() = EnterAmountAssetState(
    iconUrl = logo,
    ticker = displayTicker
)
