package com.blockchain.earn.data.dataresources.staking

import com.blockchain.api.earn.staking.StakingApiService
import com.blockchain.api.earn.staking.data.StakingRatesDto
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource

class StakingRatesStore(
    private val stakingApiService: StakingApiService
) : Store<StakingRatesDto> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofOutcome(
            mapper = {
                stakingApiService.getStakingRates()
            }
        ),
        dataSerializer = StakingRatesDto.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "StakingRatesStore"
    }
}
