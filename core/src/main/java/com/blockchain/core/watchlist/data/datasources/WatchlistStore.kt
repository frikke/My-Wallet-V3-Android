package com.blockchain.core.watchlist.data.datasources

import com.blockchain.api.services.WatchlistApiService
import com.blockchain.api.watchlist.model.WatchlistDto
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource
import com.blockchain.utils.rxSingleOutcome

class WatchlistStore internal constructor(
    private val watchlistService: WatchlistApiService
) : Store<WatchlistDto> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofSingle(
            mapper = {
                rxSingleOutcome {
                    watchlistService.getWatchlist()
                }
            }
        ),
        dataSerializer = WatchlistDto.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "WatchlistStore"
    }
}
