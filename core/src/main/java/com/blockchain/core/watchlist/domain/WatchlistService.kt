package com.blockchain.core.watchlist.domain

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import info.blockchain.balance.Currency
import kotlinx.coroutines.flow.Flow

interface WatchlistService {
    fun isAssetInWatchlist(
        asset: Currency,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<Boolean>>
}