package com.blockchain.unifiedcryptowallet.data.activity.datasource

import com.blockchain.api.selfcustody.activity.ActivityResponse
import com.blockchain.api.services.DynamicSelfCustodyService
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.KeyedFlushableDataSource
import kotlinx.serialization.Serializable

class UnifiedActivityStore(
    private val selfCustodyService: DynamicSelfCustodyService,
    private val currencyPrefs: CurrencyPrefs
) : KeyedStore<UnifiedActivityStore.Key, ActivityResponse> by PersistedJsonSqlDelightStoreBuilder()
    .buildKeyed(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofOutcome(
            mapper = { key ->
                selfCustodyService.getActivity(
                    fiatCurrency = currencyPrefs.selectedFiatCurrency.networkTicker,
                    currency = key.currency,
                    pubKey = key.pubKey,
                    acceptLanguage = key.acceptLanguage,
                    timeZone = key.timeZone,
                    nextPage = key.nextPage
                )
            }
        ),
        keySerializer = Key.serializer(),
        dataSerializer = ActivityResponse.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_1_HOUR)
    ),
    KeyedFlushableDataSource<UnifiedActivityStore.Key> {

    @Serializable
    data class Key(
        val currency: String,
        val pubKey: String,
        val acceptLanguage: String,
        val timeZone: String,
        val nextPage: String?
    )

    override fun invalidate(param: Key) {
        markAsStale(param)
    }

    override fun invalidate() {
        markStoreAsStale()
    }

    companion object {
        private const val STORE_ID = "UnifiedActivityStore"
    }
}
