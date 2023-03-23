package com.blockchain.core.recurringbuy.data.datasources

import com.blockchain.api.recurringbuy.data.RecurringBuyFrequencyConfigListDto
import com.blockchain.api.services.RecurringBuyApiService
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource

class RecurringBuyFrequencyConfigStore(
    private val recurringBuyApiService: RecurringBuyApiService
) : Store<RecurringBuyFrequencyConfigListDto> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = "RecurringBuyStore",
        fetcher = Fetcher.Keyed.ofSingle(
            mapper = {
                recurringBuyApiService.frequencyConfig()
            }
        ),
        dataSerializer = RecurringBuyFrequencyConfigListDto.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_1_HOUR)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }
}
