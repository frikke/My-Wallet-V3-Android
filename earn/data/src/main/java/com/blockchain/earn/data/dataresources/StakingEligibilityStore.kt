package com.blockchain.earn.data.dataresources

import com.blockchain.api.staking.StakingApiService
import com.blockchain.api.staking.data.StakingEligibilityDto
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

class StakingEligibilityStore(
    private val stakingApiService: StakingApiService,
) : Store<Map<String, StakingEligibilityDto>> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofOutcome(
            mapper = {
                stakingApiService.getStakingEligibility()
            }
        ),
        dataSerializer = MapSerializer(
            keySerializer = String.serializer(),
            valueSerializer = StakingEligibilityDto.serializer()
        ),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "StakingEligibilityStore"
    }
}
