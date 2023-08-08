package com.blockchain.earn.data.dataresources.staking

import com.blockchain.api.earn.staking.StakingApiService
import com.blockchain.api.earn.staking.data.StakingActivityDto
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.KeyedFlushableDataSource
import kotlinx.serialization.builtins.serializer

/**
 * Staking bonding/unbonding transactions
 */
class StakingBondingStore(
    private val stakingApiService: StakingApiService
) : KeyedStore<String, StakingActivityDto> by PersistedJsonSqlDelightStoreBuilder()
    .buildKeyed(
        storeId = "StakingBondingStore",
        fetcher = Fetcher.Keyed.ofOutcome { ticker ->
            stakingApiService.getBondingActivity(ticker)
        },
        dataSerializer = StakingActivityDto.serializer(),
        keySerializer = String.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    KeyedFlushableDataSource<String> {

    override fun invalidate() {
        markStoreAsStale()
    }

    override fun invalidate(param: String) {
        markAsStale(param)
    }
}
