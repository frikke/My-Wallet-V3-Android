package com.blockchain.core.watchlist.data

import com.blockchain.api.services.WatchlistApiService
import com.blockchain.api.services.WatchlistApiService.Companion.FAVOURITE_TAG
import com.blockchain.core.watchlist.data.datasources.WatchlistStore
import com.blockchain.core.watchlist.domain.WatchlistService
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.nabu.Authenticator
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Currency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.rx3.await
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome

typealias Watchlist = List<Currency>

class WatchlistRepository(
    private val watchlistStore: WatchlistStore,
    private val authenticator: Authenticator,
    private val watchlistApiService: WatchlistApiService,
    private val assetCatalogue: AssetCatalogue
) : WatchlistService {

    private fun getWatchlist(freshnessStrategy: FreshnessStrategy): Flow<DataResource<Watchlist>> {
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

    override suspend fun addToWatchlist(asset: Currency) {
        authenticator.authenticate {
            rxSingleOutcome {
                watchlistApiService.addToWatchlist(it.authHeader, asset.networkTicker)
            }
        }.await()

        // refresh store - will refresh any collectors of isAssetInWatchlist
        getWatchlist(FreshnessStrategy.Fresh).collect()
    }

    override suspend fun removeFromWatchlist(asset: Currency) {
        authenticator.authenticate {
            rxSingleOutcome {
                watchlistApiService.removeFromWatchlist(it.authHeader, asset.networkTicker)
            }
        }.await()

        // refresh store - will refresh any collectors of isAssetInWatchlist
        getWatchlist(FreshnessStrategy.Fresh).collect()
    }
}