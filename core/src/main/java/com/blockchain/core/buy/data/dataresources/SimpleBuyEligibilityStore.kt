package com.blockchain.core.buy.data.dataresources

import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyEligibilityDto
import com.blockchain.nabu.service.NabuService
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource

class SimpleBuyEligibilityStore(
    private val nabuService: NabuService
) : Store<SimpleBuyEligibilityDto> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofSingle(
            mapper = {
                nabuService.isEligibleForSimpleBuy()
            }
        ),
        dataSerializer = SimpleBuyEligibilityDto.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "SimpleBuyEligibilityStore"
    }
}
