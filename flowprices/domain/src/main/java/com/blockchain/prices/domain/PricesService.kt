package com.blockchain.prices.domain

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import kotlinx.coroutines.flow.Flow

interface PricesService {
    fun allAssets(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
    ): Flow<DataResource<List<AssetPriceInfo>>>
}
