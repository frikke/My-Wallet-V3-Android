package com.blockchain.core.recurringbuy.data.datasources

import com.blockchain.api.recurringbuy.data.RecurringBuyDto
import com.blockchain.api.services.RecurringBuyApiService
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.serialization.builtins.ListSerializer

class RecurringBuyStore(
    private val recurringBuyApiService: RecurringBuyApiService
) : Store<List<RecurringBuyDto>> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = "RecurringBuyStore",
        fetcher = Fetcher.Keyed.ofSingle(
            mapper = {
                recurringBuyApiService.getRecurringBuys()
            }
        ),
        dataSerializer = ListSerializer(RecurringBuyDto.serializer()),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }
}
