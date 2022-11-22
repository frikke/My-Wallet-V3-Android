package com.blockchain.core.staking.data.datasources

import com.blockchain.api.staking.StakingApiService
import com.blockchain.api.staking.data.StakingLimitsMapDto
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource

class StakingLimitsStore(
    private val stakingApiService: StakingApiService,
    private val currencyPrefs: CurrencyPrefs
) : Store<StakingLimitsMapDto> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.ofOutcome(
            mapper = {
                stakingApiService.getStakingLimits(
                    fiatTicker = currencyPrefs.selectedFiatCurrency.networkTicker
                )
            }
        ),
        dataSerializer = StakingLimitsMapDto.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_1_HOUR)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "StakingLimitsStore"
    }
}
