package com.blockchain.prices.prices

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.Coincore
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.core.buy.domain.SimpleBuyService
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.watchlist.domain.WatchlistService
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
import com.blockchain.store.mapData
import com.blockchain.store.mapListData
import com.blockchain.utils.CurrentTimeProvider
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency.BTC
import info.blockchain.balance.CryptoCurrency.ETHER
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import info.blockchain.balance.isLayer2Token
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await
import kotlin.math.absoluteValue

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
                    it.toPriceItemViewModel()
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
                        it.toPriceItemViewModel()
                    }
            }
        )
    }

    private fun AssetPriceInfo.toPriceItemViewModel(): PriceItemViewState {
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
                loadFilters()
                loadData(refresh = false)
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

                loadData(refresh = true)
            }
        }
    }

    private fun loadData(refresh: Boolean) {
        pricesJob?.cancel()
        pricesJob = viewModelScope.launch {
            pricesService.allAssets()
                .collectLatest { prices ->
                    updateState {
                        modelState.copy(
                            data = it.data.updateDataWith(prices)
                        )
                    }
                }
        }
    }

    private fun loadFilters() {
        viewModelScope.launch {
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
}

private fun Money?.format(cryptoCurrency: Currency) =
    this?.toStringWithSymbol()
        ?: Money.zero(cryptoCurrency).toStringWithSymbol()
