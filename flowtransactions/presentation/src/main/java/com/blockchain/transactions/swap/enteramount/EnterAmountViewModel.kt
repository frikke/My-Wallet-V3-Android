package com.blockchain.transactions.swap.enteramount

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.EmptyNavEvent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.componentlib.control.CurrencyValue
import com.blockchain.componentlib.control.InputCurrency
import com.blockchain.componentlib.control.flip
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.transactions.swap.SwapService
import com.blockchain.utils.removeLeadingZeros
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import java.math.BigDecimal
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * @property fromTicker if we come from Coinview we should have preset FROM
 */
class EnterAmountViewModel(
    private val fromTicker: String? = null,
    private val swapService: SwapService,
    private val exchangeRates: ExchangeRatesDataManager,
    private val currencyPrefs: CurrencyPrefs
) : MviViewModel<EnterAmountIntent, EnterAmountViewState, EnterAmountModelState, EmptyNavEvent, ModelConfigArgs.NoArgs>(
    EnterAmountModelState()
) {

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
                }
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
                                fromAccount = accounts.first { it.currency.networkTicker == "BTC" },
                                toAccount = accounts.first { it.currency.networkTicker == "DOGE" },
                                fiatAmount = Money.fromMajor(currencyPrefs.selectedFiatCurrency, BigDecimal.ZERO),
                                cryptoAmount = Money.fromMajor(
                                    accounts.first { it.currency.networkTicker == "BTC" }.currency, BigDecimal.ZERO
                                ),
                            )
                        }

                        val exchangeRate = (
                            exchangeRates.exchangeRateToUserFiatFlow(
                                accounts.first { it.currency.networkTicker == "BTC" }.currency
                            ).filter { it is DataResource.Data }.firstOrNull() as? DataResource.Data
                            )?.data

                        updateState {
                            it.copy(
                                exchangeRate = exchangeRate
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

                val a: Money? = modelState.exchangeRate?.inverse()?.convert(
                    Money.fromMajor(
                        currencyPrefs.selectedFiatCurrency,
                        intent.amount.ifEmpty { "0" }.toBigDecimal()
                    )
                )
                updateState {
                    it.copy(
                        fiatAmountUserInput = intent.amount.removeLeadingZeros(),
                        fiatAmount = intent.amount.takeIf { it.isNotEmpty() }
                            ?.let { Money.fromMajor(fiatCurrency, it.toBigDecimal()) },
                        cryptoAmountUserInput = a?.toBigDecimal()?.stripTrailingZeros()
                            ?.takeIf { it != BigDecimal.ZERO }
                            ?.toString().orEmpty(),
                        cryptoAmount = a
                    )
                }
            }
            is EnterAmountIntent.CryptoAmountChanged -> {
                val fromCurrency = modelState.fromAccount?.currency
                check(fromCurrency != null)

                val a: Money? = modelState.exchangeRate?.convert(
                    Money.fromMajor(
                        fromCurrency,
                        intent.amount.ifEmpty { "0" }.toBigDecimal()
                    )
                )

                updateState {
                    it.copy(
                        cryptoAmountUserInput = intent.amount.removeLeadingZeros(),
                        cryptoAmount = intent.amount.takeIf { it.isNotEmpty() }
                            ?.let { Money.fromMajor(fromCurrency, it.toBigDecimal()) },
                        fiatAmountUserInput = a?.toBigDecimal()?.stripTrailingZeros()
                            ?.takeIf { it != BigDecimal.ZERO }
                            ?.toString().orEmpty(),
                        fiatAmount = a
                    )
                }
            }
        }
    }
}

private fun Currency.toViewState() = EnterAmountAssetState(
    iconUrl = logo,
    ticker = displayTicker
)
