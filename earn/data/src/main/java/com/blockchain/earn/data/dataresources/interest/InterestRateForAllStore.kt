package com.blockchain.earn.data.dataresources.interest

import com.blockchain.api.earn.passive.InterestApiService
import com.blockchain.api.earn.passive.data.InterestRatesDto
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource

class InterestRateForAllStore(
    private val interestApiService: InterestApiService,
) : Store<InterestRatesDto> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofOutcome(
            mapper = {
                interestApiService.getAllInterestRates()
            }
        ),
        dataSerializer = InterestRatesDto.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "InterestRatesStore"
    }
}
