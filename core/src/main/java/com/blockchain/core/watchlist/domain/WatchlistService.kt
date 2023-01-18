package com.blockchain.core.watchlist.domain

import com.blockchain.core.watchlist.domain.model.WatchlistToggle
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import info.blockchain.balance.Currency
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow

interface WatchlistService {
    fun getWatchlist(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<List<Currency>>>

    fun isAssetInWatchlist(
        asset: Currency,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<Boolean>>

    suspend fun addToWatchlist(
        asset: Currency
    ): DataResource<Unit>

    suspend fun removeFromWatchlist(
        asset: Currency
    ): DataResource<Unit>

    suspend fun updateWatchlist(
        asset: Currency,
        toggle: WatchlistToggle
    ): DataResource<Unit>
}
