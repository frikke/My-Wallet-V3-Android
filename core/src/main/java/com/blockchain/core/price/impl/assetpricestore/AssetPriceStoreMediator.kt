package com.blockchain.core.price.impl.assetpricestore

import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.model.AssetPriceRecord
import com.blockchain.store.CachedData
import com.blockchain.store.Mediator
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator

internal object AssetPriceStoreMediator : Mediator<AssetPriceStoreCache.Key, List<AssetPriceRecord>> {
    private val nonHistoricalMediator =
        FreshnessMediator<AssetPriceStoreCache.Key, List<AssetPriceRecord>>(Freshness.ofHours(1))

    // This uses a much shorter cache because this is used to draw the graph in AssetDetails so it needs to be accurate
    private val dailyHistoricalMediator =
        FreshnessMediator<AssetPriceStoreCache.Key, List<AssetPriceRecord>>(Freshness.ofMinutes(15))
    private val otherHistoricalMediator =
        FreshnessMediator<AssetPriceStoreCache.Key, List<AssetPriceRecord>>(Freshness.ofHours(24))

    override fun shouldFetch(
        cachedData: CachedData<AssetPriceStoreCache.Key, List<AssetPriceRecord>>?
    ): Boolean = when (val key = cachedData?.key) {
        is AssetPriceStoreCache.Key.GetAllCurrent -> nonHistoricalMediator.shouldFetch(cachedData)
        is AssetPriceStoreCache.Key.GetAllYesterday -> nonHistoricalMediator.shouldFetch(cachedData)
        is AssetPriceStoreCache.Key.GetHistorical -> when (key.timeSpan) {
            HistoricalTimeSpan.DAY -> dailyHistoricalMediator.shouldFetch(cachedData)
            else -> otherHistoricalMediator.shouldFetch(cachedData)
        }
        null -> true
    }
}
