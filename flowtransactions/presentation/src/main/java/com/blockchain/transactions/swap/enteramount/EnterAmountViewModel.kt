package com.blockchain.transactions.swap.enteramount

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.EmptyNavEvent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.componentlib.control.CurrencyValue
import com.blockchain.data.DataResource
import com.blockchain.transactions.swap.SwapService
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @property fromTicker if we come from Coinview we should have preset FROM
 */
class EnterAmountViewModel(
    private val fromTicker: String? = null,
    private val swapService: SwapService
) : MviViewModel<EnterAmountIntent, EnterAmountViewState, EnterAmountModelState, EmptyNavEvent, ModelConfigArgs.NoArgs>(
    EnterAmountModelState()
) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: EnterAmountModelState): EnterAmountViewState {
        return with(state) {
            EnterAmountViewState(
                fromAsset = fromAccount?.currency?.toViewState(),
                toAsset = toAccount?.currency?.toViewState(),
                fiatAmount = fiatAmount,
                cryptoAmount = cryptoAmount
            )
        }
    }

    override suspend fun handleIntent(modelState: EnterAmountModelState, intent: EnterAmountIntent) {
        when (intent) {
            EnterAmountIntent.LoadData -> {
                viewModelScope.launch {
                    swapService.sourceAccounts()
                        .collectLatest {
                            (it as? DataResource.Data)?.data?.let { accounts->
                                updateState {
                                    it.copy(
                                        fromAccount = accounts.first { it.currency.networkTicker == "BTC" },
                                        toAccount = accounts.first { it.currency.networkTicker == "DOGE" },
                                        fiatAmount = CurrencyValue(
                                            value = "100", ticker = "$", isPrefix = true, separateWithSpace = false
                                        ),
                                        cryptoAmount = CurrencyValue(
                                            value = "200", ticker = "BTC", isPrefix = false, separateWithSpace = true
                                        ),
                                    )
                                }
                            }
                        }
                }
            }
            is EnterAmountIntent.FiatAmountChanged -> {
                check(modelState.fiatAmount != null)
                check(modelState.cryptoAmount != null)

                swapService.fiatToCrypto(intent.amount, FiatCurrency.Dollars, CryptoCurrency.ETHER)
                viewModelScope.launch {
                    val cryptoAmount = swapService.fiatToCrypto(
                        intent.amount, FiatCurrency.Dollars, CryptoCurrency.ETHER
                    )
                    updateState {
                        it.copy(
                            fiatAmount = it.fiatAmount?.copy(value = intent.amount),
                            cryptoAmount = it.cryptoAmount?.copy(value = cryptoAmount)
                        )
                    }
                }
            }
            is EnterAmountIntent.CryptoAmountChanged -> {
                check(modelState.fiatAmount != null)
                check(modelState.cryptoAmount != null)

                viewModelScope.launch {
                    val fiatAmount = swapService.cryptoToFiat(
                        intent.amount, FiatCurrency.Dollars, CryptoCurrency.ETHER
                    )

                    updateState {
                        it.copy(
                            cryptoAmount = it.cryptoAmount?.copy(value = intent.amount),
                            fiatAmount = it.fiatAmount?.copy(value = fiatAmount),
                        )
                    }
                }
            }
        }
    }
}

private fun Currency.toViewState() = EnterAmountAssetState(
    iconUrl =  logo,
    ticker =  displayTicker
)
