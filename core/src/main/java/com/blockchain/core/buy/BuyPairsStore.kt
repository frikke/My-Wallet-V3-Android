package com.blockchain.core.buy

import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyPairsResp
import com.blockchain.nabu.service.NabuService
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource

class BuyPairsStore(
    private val nabuService: NabuService
) : Store<SimpleBuyPairsResp> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofSingle(
            mapper = {
                nabuService.getSupportedCurrencies()
            }
        ),
        dataSerializer = SimpleBuyPairsResp.serializer(),
        mediator = FreshnessMediator(Freshness.ofHours(24))
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "BuyPairsStore"
    }
}
