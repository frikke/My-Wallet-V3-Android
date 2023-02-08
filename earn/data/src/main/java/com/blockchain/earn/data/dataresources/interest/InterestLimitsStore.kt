package com.blockchain.earn.data.dataresources.interest

import com.blockchain.api.earn.passive.InterestApiService
import com.blockchain.api.earn.passive.data.InterestTickerLimitsDto
import com.blockchain.logging.Logger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource

class InterestLimitsStore(
    private val interestApiService: InterestApiService,
    private val currencyPrefs: CurrencyPrefs
) : Store<InterestTickerLimitsDto> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.ofSingle(
            mapper = {
                interestApiService.getTickersLimits(
                    fiatCurrencyTicker = currencyPrefs.selectedFiatCurrency.networkTicker
                ).doOnError { Logger.e("Limits call failed $it") }
            }
        ),
        dataSerializer = InterestTickerLimitsDto.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_1_HOUR)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "InterestLimitsStore"
    }
}
