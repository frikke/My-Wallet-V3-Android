package com.blockchain.earn.data.dataresources.interest

import com.blockchain.api.earn.EarnRewardsEligibilityResponseDto
import com.blockchain.api.earn.EarnRewardsEligibilityResponseDtoSerializer
import com.blockchain.api.earn.passive.InterestApiService
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource

class InterestEligibilityStore(
    private val interestApiService: InterestApiService
) : Store<EarnRewardsEligibilityResponseDto> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.ofSingle(
            mapper = {
                interestApiService.getTickersEligibility()
            }
        ),
        dataSerializer = EarnRewardsEligibilityResponseDtoSerializer,
        mediator = FreshnessMediator(Freshness.DURATION_1_HOUR)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "InterestEligibilityStore"
    }
}
