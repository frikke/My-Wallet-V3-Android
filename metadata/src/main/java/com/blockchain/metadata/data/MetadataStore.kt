package com.blockchain.metadata.data

import com.blockchain.internalnotifications.NotificationEvent
import com.blockchain.metadata.MetadataEntry
import com.blockchain.store.CacheConfiguration
import com.blockchain.store.CachedData
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.Mediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import info.blockchain.wallet.metadata.MetadataApiService
import info.blockchain.wallet.metadata.data.MetadataResponse
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class MetadataStoreKey(@Transient val address: String = "", val type: Int)

class MetadataStore(private val metadataApiService: MetadataApiService) :
    KeyedStore<MetadataStoreKey, MetadataResponse> by PersistedJsonSqlDelightStoreBuilder().buildKeyed(
        storeId = "MetadataStorage",
        reset = CacheConfiguration.on(
            listOf(
                NotificationEvent.Logout,
                NotificationEvent.MetadataUpdated,
                NotificationEvent.PayloadUpdated
            )
        ),
        keySerializer = MetadataStoreKey.serializer(),
        dataSerializer = MetadataResponse.serializer(),
        fetcher = Fetcher.Keyed.ofSingle {
            metadataApiService.getMetadata(it.address)
        },
        mediator = MetadataMediator
    )

internal object MetadataMediator : Mediator<MetadataStoreKey, MetadataResponse> {
    override fun shouldFetch(cachedData: CachedData<MetadataStoreKey, MetadataResponse>?): Boolean {
        if (cachedData == null || cachedData.lastFetched == 0L) return true
        val entry = MetadataEntry.values().firstOrNull { it.index == cachedData.key.type } ?: return true
        return when (entry) {
            MetadataEntry.WHATS_NEW,
            MetadataEntry.BUY_SELL,
            MetadataEntry.CONTACTS,
            MetadataEntry.SHAPE_SHIFT,
            MetadataEntry.LOCKBOX -> false

            MetadataEntry.METADATA_ETH,
            MetadataEntry.METADATA_BCH,
            MetadataEntry.METADATA_XLM -> fetchIfOlderThan1Day(cachedData.lastFetched)

            MetadataEntry.BLOCKCHAIN_UNIFIED_CREDENTIALS,
            MetadataEntry.WALLET_CREDENTIALS,
            MetadataEntry.NABU_LEGACY_CREDENTIALS -> false

            MetadataEntry.WALLET_CONNECT_METADATA -> true
        }
    }

    private fun fetchIfOlderThan1Day(lastFetched: Long): Boolean {
        val age = Calendar.getInstance().timeInMillis - lastFetched
        return age > TimeUnit.DAYS.toMillis(1)
    }
}
