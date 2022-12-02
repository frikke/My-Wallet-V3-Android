package com.blockchain.nabu.datamanagers.repositories.swap

import com.blockchain.nabu.service.NabuService
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource
import com.blockchain.utils.awaitOutcome
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

class CustodialTradingPairsStore(
    private val nabuService: NabuService
) : Store<List<String>> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.ofOutcome(
            mapper = {
                nabuService.getSwapAvailablePairs().awaitOutcome()
            }
        ),
        dataSerializer = ListSerializer(String.serializer()),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "CustodialTradingPairsStore"
    }
}
