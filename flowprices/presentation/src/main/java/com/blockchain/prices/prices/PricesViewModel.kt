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
import com.blockchain.data.filter
import com.blockchain.data.map
import com.blockchain.data.mapList
import com.blockchain.data.updateDataWith
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.mapData
import com.blockchain.store.mapListData
import com.blockchain.utils.CurrentTimeProvider
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency.BTC
import info.blockchain.balance.CryptoCurrency.ETHER
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await

class PricesViewModel(
    private val currencyPrefs: CurrencyPrefs,
    private val coincore: Coincore,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val simpleBuyService: SimpleBuyService,
    private val watchlistService: WatchlistService,
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
                .filter { assetPriceInfo ->
                    state.filterTerm.isEmpty() ||
                        assetPriceInfo.assetInfo.displayTicker.contains(state.filterTerm, ignoreCase = true) ||
                        assetPriceInfo.assetInfo.name.contains(state.filterTerm, ignoreCase = true)
                }
                .filter { assetPriceInfo ->
                    when (state.filterBy) {
                        PricesFilter.All -> {
                            true
                        }
                        PricesFilter.Tradable -> {
                            state.tradableCurrencies.map { tradable ->
                                assetPriceInfo.assetInfo.networkTicker in tradable
                            }.dataOrElse(false)
                        }
                        PricesFilter.Favorites -> {
                            state.watchlist.map { watchlist ->
                                assetPriceInfo.assetInfo.networkTicker in watchlist
                            }.dataOrElse(false)
                        }
                    }
                }
                .map { list ->
                    /**
                     * sorted by: watchlist - asset index - is tradable - marketcap
                     */
                    list.sortedWith(
                        compareByDescending<AssetPriceInfo> {
                            state.watchlist.map { watchlist ->
                                it.assetInfo.networkTicker in watchlist
                            }.dataOrElse(false)
                        }.thenByDescending {
                            it.assetInfo.index
                        }.thenByDescending {
                            state.tradableCurrencies.map { tradable ->
                                it.assetInfo.networkTicker in tradable
                            }.dataOrElse(false)
                        }.thenByDescending {
                            it.price.map { price -> price.marketCap }.dataOrElse(null)
                        }.thenBy {
                            it.assetInfo.name
                        }
                    )
                }
                .mapList {
                    it.toPriceItemViewModel()
                }
        )
    }

    private fun AssetPriceInfo.toPriceItemViewModel(): PriceItemViewState {
        return PriceItemViewState(
            asset = assetInfo,
            name = assetInfo.name,
            ticker = assetInfo.displayTicker,
            network = assetInfo.coinNetwork?.shortName,
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
                loadData()
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

                onIntent(PricesIntents.LoadData(forceRefresh = true))
            }
        }
    }

    private fun loadData() {
        pricesJob?.cancel()
        pricesJob = viewModelScope.launch {
            val tradableCurrenciesFlow = simpleBuyService.getSupportedBuySellCryptoCurrencies()
                .mapListData { it.source.networkTicker }

            val watchlistFlow = watchlistService.getWatchlist()
                .mapData { (it + defaultWatchlist).distinct() }.mapListData { it.networkTicker }

            val pricesFlow = loadAssetsAndPrices()

            combine(
                tradableCurrenciesFlow,
                watchlistFlow,
                pricesFlow
            ) { tradableCurrencies, watchlist, prices ->
                Triple(tradableCurrencies, watchlist, prices)
            }.collectLatest { (tradableCurrencies, watchlist, prices) ->
                updateState {
                    modelState.copy(
                        tradableCurrencies = it.tradableCurrencies.updateDataWith(tradableCurrencies),
                        watchlist = it.watchlist.updateDataWith(watchlist),
                        data = it.data.updateDataWith(prices)
                    )
                }
            }
        }
    }

    private fun loadFilters() {
        // atm same filters for both modes
        updateState {
            it.copy(
                filters = listOf(
                    PricesFilter.All, PricesFilter.Favorites, PricesFilter.Tradable
                )
            )
        }
    }

    private suspend fun loadAssetsAndPrices(): Flow<DataResource<List<AssetPriceInfo>>> {
        // todo(othman) have to check why flow is behaving strange - for now keeping rx

        //        return coincore.availableCryptoAssetsFlow().flatMapData {
        //            val assetPriceInfoList = it.map { assetInfo ->
        //                exchangeRatesDataManager
        //                    .getPricesWith24hDelta(
        //                        assetInfo,
        //                        FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
        //                    )
        //                    .filterNotLoading()
        //                    .map { priceDataResource ->
        //                        AssetPriceInfo(
        //                            price = priceDataResource,
        //                            assetInfo = assetInfo
        //                        )
        //                    }
        //
        //            }
        //
        //            combine(assetPriceInfoList) {
        //                println("---------- combine ${it.size} ${it.count { it.price is DataResource.Loading }}")
        //
        //                it.toList()
        //            }.map {
        //                //                if (it.any { it.price is DataResource.Loading }) {
        //                //                    DataResource.Loading
        //                //                } else {
        //                DataResource.Data(it)
        //                //                }
        //
        //            }
        //        }
        return flowOf(
            coincore.availableCryptoAssets()
                .flatMap { assets ->
                    Single.concat(
                        assets.map { fetchAssetPrice(it) }
                    ).toList()
                }
                .map {
                    DataResource.Data(it)
                }
                .await()
        )
    }

    private fun fetchAssetPrice(assetInfo: AssetInfo): Single<AssetPriceInfo> {
        return exchangeRatesDataManager.getPricesWith24hDeltaLegacy(assetInfo).firstOrError()
            .map { prices24HrWithDelta ->
                AssetPriceInfo(
                    price = DataResource.Data(prices24HrWithDelta),
                    assetInfo = assetInfo
                )
            }.subscribeOn(Schedulers.io()).onErrorReturn {
                AssetPriceInfo(
                    price = DataResource.Error(Exception()),
                    assetInfo = assetInfo
                )
            }
    }

    companion object {
        val defaultWatchlist = listOf(BTC, ETHER)
    }
}

private fun Money?.format(cryptoCurrency: Currency) =
    this?.toStringWithSymbol()
        ?: Money.zero(cryptoCurrency).toStringWithSymbol()
