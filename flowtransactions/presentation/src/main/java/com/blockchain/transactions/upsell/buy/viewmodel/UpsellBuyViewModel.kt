package com.blockchain.transactions.upsell.buy.viewmodel

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.componentlib.tablerow.BalanceChange
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.core.buy.domain.SimpleBuyService
import com.blockchain.data.filter
import com.blockchain.data.map
import com.blockchain.data.mapList
import com.blockchain.data.toImmutableList
import com.blockchain.prices.domain.AssetPriceInfo
import com.blockchain.prices.domain.PricesService
import com.blockchain.prices.prices.PriceItemViewState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UpsellBuyViewModel(
    private val assetJustTransactedTicker: String,
    private val pricesService: PricesService,
    private val simpleBuyService: SimpleBuyService,
) : MviViewModel<
    UpsellBuyIntent,
    UpsellBuyViewState,
    UpsellBuyModelState,
    UpsellBuyNavigationEvent,
    ModelConfigArgs.NoArgs
    >(
    initialState = UpsellBuyModelState()
) {
    private var mostPopularJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun UpsellBuyModelState.reduce() = UpsellBuyViewState(
        assetsToUpSell = assetsToUpSell.mapList {
            it.toPriceItemViewState()
        }.toImmutableList(),
        isLoading = isLoading
    )

    override suspend fun handleIntent(modelState: UpsellBuyModelState, intent: UpsellBuyIntent) {
        when (intent) {
            is UpsellBuyIntent.LoadData -> {
                loadMostPopularAssets()
            }

            is UpsellBuyIntent.DismissUpsell -> {
                simpleBuyService.dismissUpsellBuy()
            }
        }
    }

    private fun loadMostPopularAssets() {
        mostPopularJob?.cancel()
        mostPopularJob = viewModelScope.launch {
            pricesService.mostPopularAssets()
                .collectLatest { mostPopularAssets ->
                    updateState {
                        copy(
                            assetsToUpSell = mostPopularAssets.filter { asset ->
                                asset.assetInfo.networkTicker != assetJustTransactedTicker
                            },
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun AssetPriceInfo.toPriceItemViewState(): PriceItemViewState {
        return PriceItemViewState(
            asset = assetInfo,
            data = BalanceChange(
                name = assetInfo.name,
                ticker = assetInfo.displayTicker,
                network = null,
                logo = assetInfo.logo,
                delta = price.map { ValueChange.fromValue(it.delta24h) },
                currentPrice = price.map {
                    it.currentRate.price.toStringWithSymbol()
                },
                showRisingFastTag = false
            )
        )
    }
}
