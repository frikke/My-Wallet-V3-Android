package com.blockchain.core.buy.data.dataresources

import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyPairsDto
import com.blockchain.nabu.service.NabuService
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.IsCachedMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource

class BuyPairsStore(
    private val nabuService: NabuService
) : Store<SimpleBuyPairsDto> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofSingle(
            mapper = {
                nabuService.getSupportedCurrencies()
            }
        ),
        dataSerializer = SimpleBuyPairsDto.serializer(),
        mediator = IsCachedMediator()
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "BuyPairsStore"
    }
}
