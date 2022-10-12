package com.blockchain.core.chains.dynamicselfcustody.data

import com.blockchain.api.selfcustody.GetSubscriptionsResponse
import com.blockchain.api.services.DynamicSelfCustodyService
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource

internal class NonCustodialSubscriptionsStore(
    private val dynamicSelfCustodyService: DynamicSelfCustodyService,
) : Store<GetSubscriptionsResponse> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.ofOutcome(
            mapper = {
                dynamicSelfCustodyService.getSubscriptions()
            }
        ),
        dataSerializer = GetSubscriptionsResponse.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "NonCustodialSubscriptionsStore"
    }
}
