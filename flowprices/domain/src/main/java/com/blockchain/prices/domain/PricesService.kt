package com.blockchain.prices.domain

import com.blockchain.data.DataResource
import kotlinx.coroutines.flow.Flow

interface PricesService {
    fun allAssets(): Flow<DataResource<List<AssetPriceInfo>>>
    fun tradableAssets(): Flow<DataResource<List<AssetPriceInfo>>>
    fun assets(tickers: List<String>): Flow<DataResource<List<AssetPriceInfo>>>
    fun topMoversCount(): Flow<Int>
    fun mostPopularTickers(): Flow<List<String>>
    fun risingFastPercentThreshold(): Flow<Double>

    fun mostPopularAssets(): Flow<DataResource<List<AssetPriceInfo>>>
}
