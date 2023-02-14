package com.blockchain.prices

import com.blockchain.coincore.Coincore
import com.blockchain.core.buy.domain.SimpleBuyService
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.core.watchlist.domain.WatchlistService
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.dataOrElse
import com.blockchain.data.map
import com.blockchain.data.mapList
import com.blockchain.extensions.minus
import com.blockchain.prices.domain.AssetPriceInfo
import com.blockchain.prices.domain.PricesService
import com.blockchain.store.mapData
import com.blockchain.store.mapListData
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.rx3.await

class PricesRepository(
    private val coincore: Coincore,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val simpleBuyService: SimpleBuyService,
    private val watchlistService: WatchlistService
) : PricesService {
    override fun allAssets(
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<List<AssetPriceInfo>>> {
        val tradableCurrenciesFlow = simpleBuyService.getSupportedBuySellCryptoCurrencies(
            freshnessStrategy = freshnessStrategy
        ).mapListData { it.source.networkTicker }

        val watchlistFlow = watchlistService.getWatchlist(
            freshnessStrategy = freshnessStrategy
        ).mapData { (it + defaultWatchlist).distinct() }.mapListData { it.networkTicker }

        val pricesFlow = loadAssetsAndPrices()

        return combine(
            tradableCurrenciesFlow,
            watchlistFlow,
            pricesFlow
        ) { tradableCurrencies, watchlist, prices ->
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

    private fun loadAssetsAndPrices(): Flow<DataResource<Map<AssetInfo, DataResource<Prices24HrWithDelta>>>> {
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

    companion object {
        private val defaultWatchlist = listOf(CryptoCurrency.BTC, CryptoCurrency.ETHER)
    }
}
