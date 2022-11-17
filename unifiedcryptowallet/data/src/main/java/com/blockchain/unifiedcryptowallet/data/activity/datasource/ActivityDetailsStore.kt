package com.blockchain.unifiedcryptowallet.data.activity.datasource

import com.blockchain.api.selfcustody.activity.ActivityDetailsResponse
import com.blockchain.api.services.DynamicSelfCustodyService
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.KeyedFlushableDataSource
import kotlinx.serialization.Serializable

class ActivityDetailsStore(
    private val selfCustodyService: DynamicSelfCustodyService,
    private val currencyPrefs: CurrencyPrefs
) : KeyedStore<ActivityDetailsStore.Key, ActivityDetailsResponse> by PersistedJsonSqlDelightStoreBuilder()
    .buildKeyed(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofOutcome(
            mapper = { key ->
                selfCustodyService.getActivityDetails(
                    txId = key.txId,
                    network = key.network,
                    pubKey = key.pubKey,
                    timeZone = key.timeZone,
                    locales = key.locales,
                    fiatCurrency = currencyPrefs.selectedFiatCurrency.networkTicker
                )
            }
        ),
        keySerializer = Key.serializer(),
        dataSerializer = ActivityDetailsResponse.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    KeyedFlushableDataSource<ActivityDetailsStore.Key> {

    @Serializable
    data class Key(
        val txId: String,
        val network: String,
        val pubKey: String,
        val locales: String,
        val timeZone: String
    )

    override fun invalidate(param: Key) {
        markAsStale(param)
    }

    override fun invalidate() {
        markStoreAsStale()
    }

    companion object {
        private const val STORE_ID = "UnifiedActivityDetailsStore"
    }
}
