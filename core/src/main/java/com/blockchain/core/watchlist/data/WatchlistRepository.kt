package com.blockchain.core.watchlist.data

import com.blockchain.core.watchlist.data.datasources.WatchlistStore
import com.blockchain.core.watchlist.domain.WatchlistService
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Currency
import kotlinx.coroutines.flow.Flow

typealias Watchlist = List<Currency>

class WatchlistRepository(
    private val watchlistStore: WatchlistStore,
    private val assetCatalogue: AssetCatalogue
) : WatchlistService {

    private fun getWatchlist(freshnessStrategy: FreshnessStrategy): Flow<DataResource<Watchlist>> {
        return watchlistStore.stream(freshnessStrategy).mapData { watchlistDto ->
            val watchlist = mutableListOf<Currency>()

            watchlistDto.items.forEach { item ->
                assetCatalogue.fromNetworkTicker(item.asset)?.let { currency ->
                    if (item.tags.any { it.tag == "Favourite" }) {
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
}

// ///////////////
// EXTENSIONS
// ///////////////
