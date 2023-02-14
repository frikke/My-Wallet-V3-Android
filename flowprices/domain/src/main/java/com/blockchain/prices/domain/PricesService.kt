package com.blockchain.prices.domain

import com.blockchain.data.DataResource
import kotlinx.coroutines.flow.Flow

interface PricesService {
    fun allAssets(): Flow<DataResource<List<AssetPriceInfo>>>
    fun topMoversCount(): Flow<Int>
}
