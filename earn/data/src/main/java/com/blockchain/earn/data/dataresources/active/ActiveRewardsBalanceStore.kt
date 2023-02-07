package com.blockchain.earn.data.dataresources.active

import com.blockchain.api.earn.active.ActiveRewardsApiService
import com.blockchain.api.earn.active.data.ActiveRewardsBalanceDto
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

class ActiveRewardsBalanceStore(
    private val activeRewardsApiService: ActiveRewardsApiService,
) : Store<Map<String, ActiveRewardsBalanceDto>> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofOutcome(
            mapper = {
                activeRewardsApiService.getActiveRewardsBalances()
            }
        ),
        dataSerializer = MapSerializer(
            keySerializer = String.serializer(),
            valueSerializer = ActiveRewardsBalanceDto.serializer()
        ),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "ActiveRewardsBalanceStore"
    }
}
