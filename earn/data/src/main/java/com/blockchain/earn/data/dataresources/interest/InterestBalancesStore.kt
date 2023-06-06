package com.blockchain.earn.data.dataresources.interest

import com.blockchain.api.earn.passive.InterestApiService
import com.blockchain.api.earn.passive.data.InterestAccountBalanceDto
import com.blockchain.internalnotifications.NotificationEvent
import com.blockchain.store.CacheConfiguration
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

class InterestBalancesStore(
    private val interestApiService: InterestApiService
) : Store<Map<String, InterestAccountBalanceDto>> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        reset = CacheConfiguration.on(listOf(NotificationEvent.RewardsTransaction)),
        storeId = STORE_ID,
        fetcher = Fetcher.ofSingle(
            mapper = {
                interestApiService.getAccountBalances()
            }
        ),
        dataSerializer = MapSerializer(
            keySerializer = String.serializer(),
            valueSerializer = InterestAccountBalanceDto.serializer()
        ),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "InterestBalancesStore"
    }
}
