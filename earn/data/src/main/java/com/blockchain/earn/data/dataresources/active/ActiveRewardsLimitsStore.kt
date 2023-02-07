package com.blockchain.earn.data.dataresources.active

import com.blockchain.api.earn.active.ActiveRewardsApiService
import com.blockchain.api.earn.active.data.ActiveRewardsLimitsMapDto
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource

class ActiveRewardsLimitsStore(
    private val activeRewardsApiService: ActiveRewardsApiService,
    private val currencyPrefs: CurrencyPrefs
) : Store<ActiveRewardsLimitsMapDto> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.ofOutcome(
            mapper = {
                activeRewardsApiService.getActiveRewardsLimits(
                    fiatTicker = currencyPrefs.selectedFiatCurrency.networkTicker
                )
            }
        ),
        dataSerializer = ActiveRewardsLimitsMapDto.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_1_HOUR)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "ActiveRewardsLimitsStore"
    }
}
