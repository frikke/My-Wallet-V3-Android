package com.blockchain.prices

import com.blockchain.coincore.Coincore
import com.blockchain.core.buy.domain.SimpleBuyService
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.core.watchlist.domain.WatchlistService
import com.blockchain.data.DataResource
import com.blockchain.data.dataOrElse
import com.blockchain.data.map
import com.blockchain.data.mapList
import com.blockchain.domain.experiments.RemoteConfigService
import com.blockchain.prices.domain.AssetPriceInfo
import com.blockchain.prices.domain.PricesService
import com.blockchain.store.mapData
import com.blockchain.store.mapListData
import com.blockchain.utils.toFlowDataResource
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.await

class PricesRepository(
    private val coincore: Coincore,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val simpleBuyService: SimpleBuyService,
    private val watchlistService: WatchlistService,
    private val remoteConfigService: RemoteConfigService
) : PricesService {

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun assets(tradableOnly: Boolean): Flow<DataResource<List<AssetPriceInfo>>> {
        val tradableCurrenciesAndPricesFlow = simpleBuyService.getSupportedBuySellCryptoCurrencies()
            .mapListData { it.source.networkTicker }
            .flatMapLatest { tradablePickers ->
                loadAssetsAndPrices(
                    filterOnlyPickers = tradablePickers.dataOrElse(emptyList()).takeIf { tradableOnly }
                ).map { prices ->
                    tradablePickers to prices
                }
            }

        val watchlistFlow = watchlistService.getWatchlist()
            .mapData { (it + defaultWatchlist).distinct() }.mapListData { it.networkTicker }

        return combine(
            tradableCurrenciesAndPricesFlow,
            watchlistFlow
        ) { (tradableCurrencies, prices), watchlist ->
            prices.map { it.toList() }
                .mapList { (asset, price) ->
                    AssetPriceInfo(
                        price = price,
                        assetInfo = asset,
                        isTradable = tradableCurrencies.map { asset.networkTicker in it }.dataOrElse(false),
                        isInWatchlist = watchlist.map { asset.networkTicker in it }.dataOrElse(false)
                    )
                }
        }.distinctUntilChanged()
    }

    override fun allAssets(): Flow<DataResource<List<AssetPriceInfo>>> {
        return assets(tradableOnly = false)
    }

    override fun tradableAssets(): Flow<DataResource<List<AssetPriceInfo>>> {
        return assets(tradableOnly = true)
    }

    override fun mostPopularAssets(): Flow<DataResource<List<AssetPriceInfo>>> =
        combine(mostPopularTickers(), allAssets()) {
                mostPopularTickers, allAssetsData ->
            allAssetsData.map { allAssets ->
                allAssets.filter { it.assetInfo.networkTicker in mostPopularTickers }
            }
        }

    /**
     * @param filterOnlyPickers use to load only for select assets
     * loading individual prices all and then filtering takes much more time - null to load all
     */
    private fun loadAssetsAndPrices(
        filterOnlyPickers: List<String>?
    ): Flow<DataResource<Map<AssetInfo, DataResource<Prices24HrWithDelta>>>> {
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
        return flow {
            coincore.availableCryptoAssets()
                .map { allAssets ->
                    filterOnlyPickers?.let {
                        allAssets.filter { filterOnlyPickers.contains(it.networkTicker) }
                    } ?: allAssets
                }
                .flatMap { assets ->
                    Single.concat(
                        assets.map { asset ->
                            fetchAssetPrice(asset).map { asset to it }
                        }
                    ).toMap(
                        /* keySelector = */ { (asset, _) -> asset },
                        /* valueSelector = */ { (_, price) -> price }
                    )
                }
                .map {
                    DataResource.Data(it)
                }
                .await()
                .also { emit(it) }
        }
    }

    private fun fetchAssetPrice(assetInfo: AssetInfo): Single<DataResource<Prices24HrWithDelta>> {
        return exchangeRatesDataManager.getPricesWith24hDeltaLegacy(assetInfo).firstOrError()
            .map<DataResource<Prices24HrWithDelta>> { prices24HrWithDelta ->
                DataResource.Data(prices24HrWithDelta)
            }.subscribeOn(Schedulers.io()).onErrorReturn {
                DataResource.Error(Exception())
            }
    }

    override fun topMoversCount(): Flow<Int> {
        return remoteConfigService.getRawJson(KEY_TOP_MOVERS_COUNT)
            .toFlowDataResource()
            .map {
                it.map { it.toIntOrNull() ?: TOP_MOVERS_DEFAULT_COUNT }
                    .dataOrElse(TOP_MOVERS_DEFAULT_COUNT)
            }
    }

    override fun mostPopularTickers(): Flow<List<String>> {
        return remoteConfigService.getRawJson(KEY_MOST_POPULAR)
            .toFlowDataResource()
            .map {
                it.map { it.split(MOST_POPULAR_SEPARATOR).map { it.trim() } }
                    .dataOrElse(emptyList())
            }
    }

    override fun risingFastPercentThreshold(): Flow<Double> {
        return remoteConfigService.getRawJson(KEY_PRICES_RISING_FAST_PERCENT)
            .toFlowDataResource()
            .map {
                it.map { it.toDoubleOrNull() ?: PRICES_RISING_FAST_DEFAULT_PERCENT }
                    .dataOrElse(PRICES_RISING_FAST_DEFAULT_PERCENT)
            }
    }

    companion object {
        private val defaultWatchlist = listOf(CryptoCurrency.BTC, CryptoCurrency.ETHER)
        private const val KEY_TOP_MOVERS_COUNT = "prices_top_movers_count"
        private const val TOP_MOVERS_DEFAULT_COUNT = 4
        private const val KEY_MOST_POPULAR = "prices_most_popular"
        private const val MOST_POPULAR_SEPARATOR = ","
        private const val KEY_PRICES_RISING_FAST_PERCENT = "blockchain_app_configuration_prices_rising_fast_percent"
        private const val PRICES_RISING_FAST_DEFAULT_PERCENT = 4.0
    }
}
