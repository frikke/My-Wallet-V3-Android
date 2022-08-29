package com.blockchain.core.watchlist.domain

import com.blockchain.core.watchlist.domain.model.WatchlistToggle
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import info.blockchain.balance.Currency
import kotlinx.coroutines.flow.Flow

interface WatchlistService {
    fun isAssetInWatchlist(
        asset: Currency,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<Boolean>>

    suspend fun addToWatchlist(
        asset: Currency
    ) : Flow<DataResource<Unit>>

    suspend fun removeFromWatchlist(
        asset: Currency
    ) : Flow<DataResource<Unit>>

    suspend fun updateWatchlist(
        asset: Currency,
        toggle: WatchlistToggle
    ) : Flow<DataResource<Unit>>


}