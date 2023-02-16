package com.blockchain.prices.prices

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.data.DataResource
import com.blockchain.data.dataOrElse
import com.blockchain.data.doOnData
import com.blockchain.data.filter
import com.blockchain.data.map
import com.blockchain.data.mapList
import com.blockchain.data.onErrorReturn
import com.blockchain.data.updateDataWith
import com.blockchain.nabu.Feature
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.prices.domain.AssetPriceInfo
import com.blockchain.prices.domain.PricesService
import com.blockchain.utils.CurrentTimeProvider
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import info.blockchain.balance.isLayer2Token
import kotlin.math.absoluteValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PricesViewModel(
    private val currencyPrefs: CurrencyPrefs,
    private val userFeaturePermissionService: UserFeaturePermissionService,
    private val pricesService: PricesService,
) : MviViewModel<PricesIntents,
    PricesViewState,
    PricesModelState,
    NavigationEvent,
    ModelConfigArgs.NoArgs>(
    PricesModelState()
) {

    private var pricesJob: Job? = null
    private var topMoversCountJob: Job? = null
    private var mostPopularJob: Job? = null
    private var filtersJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: PricesModelState): PricesViewState {
        return PricesViewState(
            availableFilters = state.filters,
            selectedFilter = state.filterBy,
            data = state.data
                .filter { asset ->
                    state.filterTerm.isEmpty() ||
                        asset.assetInfo.displayTicker.contains(state.filterTerm, ignoreCase = true) ||
                        asset.assetInfo.name.contains(state.filterTerm, ignoreCase = true)
                }
                .filter { asset ->
                    when (state.filterBy) {
                        PricesFilter.All -> {
                            true
                        }
                        PricesFilter.Tradable -> {
                            asset.isTradable
                        }
                        PricesFilter.Favorites -> {
                            asset.isInWatchlist
                        }
                    }
                }
                .map { list ->
                    /**
                     * sorted by: watchlist - asset index - is tradable - marketcap
                     */
                    list.sortedWith(
                        compareByDescending<AssetPriceInfo> { asset ->
                            asset.isInWatchlist
                        }.thenByDescending { asset ->
                            asset.assetInfo.index
                        }.thenByDescending { asset ->
                            asset.isTradable
                        }.thenByDescending { asset ->
                            asset.price.map { price -> price.marketCap }.dataOrElse(null)
                        }.thenBy {
                            it.assetInfo.name
                        }
                    )
                }
                .mapList {
                    it.toPriceItemViewState()
                }
                .map {
                    it.groupBy {
                        if (it.ticker in state.mostPopularTickers) PricesOutputGroup.MostPopular
                        else PricesOutputGroup.Others
                    }
                },
            topMovers = state.data.map { list ->
                list.filter { it.price is DataResource.Data && it.isTradable }
                    .sortedWith(
                        compareByDescending { asset ->
                            asset.price.map { it.delta24h.absoluteValue }.dataOrElse(0.0)
                        }
                    )
                    .take(state.topMoversCount)
                    .map {
                        it.toPriceItemViewState()
                    }
            }
        )
    }

    private fun AssetPriceInfo.toPriceItemViewState(): PriceItemViewState {
        return PriceItemViewState(
            asset = assetInfo,
            name = assetInfo.name,
            ticker = assetInfo.displayTicker,
            network = assetInfo.takeIf { it.isLayer2Token }?.coinNetwork?.shortName,
            logo = assetInfo.logo,
            delta = price.map { ValueChange.fromValue(it.delta24h) },
            currentPrice = price.map {
                it.currentRate.price.format(currencyPrefs.selectedFiatCurrency)
            }
        )
    }

    override suspend fun handleIntent(modelState: PricesModelState, intent: PricesIntents) {
        when (intent) {
            is PricesIntents.LoadData -> {
                updateState {
                    it.copy(loadStrategy = intent.strategy)
                }

                loadFilters()
                loadData(intent.strategy)
                loadTopMoversCount()
                loadMostPopularTickers()
            }

            is PricesIntents.FilterSearch -> {
                updateState {
                    it.copy(filterTerm = intent.term)
                }
            }

            is PricesIntents.Filter -> {
                updateState {
                    it.copy(filterBy = intent.filter)
                }
            }

            PricesIntents.Refresh -> {
                updateState {
                    it.copy(lastFreshDataTime = CurrentTimeProvider.currentTimeMillis())
                }
                loadData(strategy = modelState.loadStrategy)
            }
        }
    }

    private fun loadData(strategy: PricesLoadStrategy) {
        pricesJob?.cancel()
        pricesJob = viewModelScope.launch {
            val assetsFlow = when (strategy) {
                is PricesLoadStrategy.All -> pricesService.allAssets()
                is PricesLoadStrategy.TradableOnly -> pricesService.tradableAssets()
            }

            assetsFlow.collectLatest { prices ->
                updateState {
                    modelState.copy(
                        data = it.data.updateDataWith(prices)
                    )
                }
            }
        }
    }

    private fun loadFilters() {
        filtersJob?.cancel()
        filtersJob = viewModelScope.launch {
            userFeaturePermissionService.isEligibleFor(Feature.CustodialAccounts)
                .onErrorReturn { true }.doOnData { canTrade ->
                    updateState {
                        it.copy(
                            filters = listOfNotNull(
                                PricesFilter.All,
                                PricesFilter.Favorites,
                                if (canTrade) PricesFilter.Tradable else null
                            )
                        )
                    }
                }.collect()
        }
    }

    private fun loadTopMoversCount() {
        topMoversCountJob?.cancel()
        topMoversCountJob = viewModelScope.launch {
            pricesService.topMoversCount()
                .collectLatest { count ->
                    updateState {
                        it.copy(
                            topMoversCount = count
                        )
                    }
                }
        }
    }

    private fun loadMostPopularTickers() {
        mostPopularJob?.cancel()
        mostPopularJob = viewModelScope.launch {
            pricesService.mostPopularTickers()
                .collectLatest { mostPopularTickers ->
                    updateState {
                        it.copy(
                            mostPopularTickers = mostPopularTickers
                        )
                    }
                }
        }
    }
}

private fun Money?.format(cryptoCurrency: Currency) =
    this?.toStringWithSymbol()
        ?: Money.zero(cryptoCurrency).toStringWithSymbol()
