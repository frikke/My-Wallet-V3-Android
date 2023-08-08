package com.blockchain.earn.data.dataresources.active

import com.blockchain.api.earn.EarnRewardsEligibilityDto
import com.blockchain.api.earn.active.ActiveRewardsApiService
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

class ActiveRewardsEligibilityStore(
    private val activeRewardsApiService: ActiveRewardsApiService
) : Store<Map<String, EarnRewardsEligibilityDto>> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofOutcome(
            mapper = {
                activeRewardsApiService.getActiveRewardsEligibility()
            }
        ),
        dataSerializer = MapSerializer(
            keySerializer = String.serializer(),
            valueSerializer = EarnRewardsEligibilityDto.serializer()
        ),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "ActiveRewardsEligibilityStore"
    }
}
