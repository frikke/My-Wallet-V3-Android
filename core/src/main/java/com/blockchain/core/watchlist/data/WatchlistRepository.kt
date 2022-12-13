package com.blockchain.core.watchlist.data

import com.blockchain.api.services.WatchlistApiService
import com.blockchain.api.services.WatchlistApiService.Companion.FAVOURITE_TAG
import com.blockchain.core.watchlist.data.datasources.WatchlistStore
import com.blockchain.core.watchlist.domain.WatchlistService
import com.blockchain.core.watchlist.domain.model.WatchlistToggle
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.doOnData
import com.blockchain.data.map
import com.blockchain.outcome.toDataResource
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Currency
import kotlinx.coroutines.flow.Flow

class WatchlistRepository(
    private val watchlistStore: WatchlistStore,
    private val watchlistApiService: WatchlistApiService,
    private val assetCatalogue: AssetCatalogue
) : WatchlistService {

    override fun getWatchlist(freshnessStrategy: FreshnessStrategy): Flow<DataResource<List<Currency>>> {
        return watchlistStore.stream(freshnessStrategy).mapData { watchlistDto ->
            val watchlist = mutableListOf<Currency>()

            watchlistDto.items.forEach { item ->
                assetCatalogue.fromNetworkTicker(item.asset)?.let { currency ->
                    if (item.tags.any { it.tag == FAVOURITE_TAG }) {
                        watchlist.add(currency)
                    }
                }
            }
            watchlist
        }
    }

    override fun isAssetInWatchlist(
        asset: Currency,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<Boolean>> {
        return getWatchlist(freshnessStrategy).mapData { watchlist ->
            watchlist.contains(asset)
        }
    }

    override suspend fun addToWatchlist(asset: Currency): DataResource<Unit> {
        return watchlistApiService.addToWatchlist(asset.networkTicker).toDataResource()
            .doOnData { watchlistStore.invalidate() }
            .map { /*Unit we don't care about return*/ }
    }

    override suspend fun removeFromWatchlist(asset: Currency): DataResource<Unit> {
        return watchlistApiService.removeFromWatchlist(asset.networkTicker).toDataResource()
            .doOnData { watchlistStore.invalidate() }
            .map { /*Unit we don't care about return*/ }
    }

    override suspend fun updateWatchlist(
        asset: Currency,
        toggle: WatchlistToggle
    ): DataResource<Unit> {
        return when (toggle) {
            WatchlistToggle.ADD -> addToWatchlist(asset)
            WatchlistToggle.REMOVE -> removeFromWatchlist(asset)
        }
    }
}
