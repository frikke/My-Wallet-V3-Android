package com.blockchain.transactions.swap.selecttarget

import com.blockchain.commonarch.presentation.mvi_v2.EmptyNavEvent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.componentlib.tablerow.BalanceChange
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.data.dataOrElse
import com.blockchain.data.map
import com.blockchain.data.mapList
import com.blockchain.data.updateDataWith
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.prices.domain.AssetPriceInfo
import com.blockchain.prices.domain.PricesService
import com.blockchain.transactions.swap.SwapService
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import info.blockchain.balance.isLayer2Token
import kotlinx.coroutines.flow.collectLatest

class SelectTargetViewModel(
    private val sourceTicker: String,
    private val swapService: SwapService,
    private val pricesService: PricesService,
    private val currencyPrefs: CurrencyPrefs,
) : MviViewModel<SelectTargetIntent,
    SelectTargetViewState,
    SelectTargetModelState,
    EmptyNavEvent,
    ModelConfigArgs.NoArgs>(
    SelectTargetModelState()
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: SelectTargetModelState) = state.run {
        SelectTargetViewState(
            prices = prices.mapList { it.toViewState() }
        )
    }

    override suspend fun handleIntent(modelState: SelectTargetModelState, intent: SelectTargetIntent) {
        when (intent) {
            is SelectTargetIntent.LoadData -> {
                swapService.targetTickers(sourceTicker).let { tickers ->
                    pricesService.assets(tickers)
                        .collectLatest { pricesData ->
                            updateState {
                                it.copy(
                                    prices = it.prices.updateDataWith(pricesData)
                                )
                            }
                        }
                }
            }

            is SelectTargetIntent.AssetSelected -> {
                // todo if asset has multiple accounts show account selection
            }
        }
    }

    private fun AssetPriceInfo.toViewState(
        withNetwork: Boolean = true
    ): BalanceChange {
        return BalanceChange(
            name = assetInfo.name,
            ticker = assetInfo.networkTicker,
            network = assetInfo.takeIf { it.isLayer2Token }?.coinNetwork?.shortName?.takeIf { withNetwork },
            logo = assetInfo.logo,
            delta = price.map { ValueChange.fromValue(it.delta24h) },
            currentPrice = price.map {
                it.currentRate.price.format(currencyPrefs.selectedFiatCurrency)
            },
            showRisingFastTag = false
        )
    }
}

private fun Money?.format(cryptoCurrency: Currency) =
    this?.toStringWithSymbol()
        ?: Money.zero(cryptoCurrency).toStringWithSymbol()
