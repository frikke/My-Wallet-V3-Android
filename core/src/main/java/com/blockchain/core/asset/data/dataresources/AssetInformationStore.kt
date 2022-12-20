package com.blockchain.core.asset.data.dataresources

import com.blockchain.api.assetdiscovery.data.AssetInformationDto
import com.blockchain.api.services.AssetDiscoveryApiService
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.KeyedFlushableDataSource
import kotlinx.serialization.Serializable

class AssetInformationStore(
    private val discoveryService: AssetDiscoveryApiService
) : KeyedStore<AssetInformationStore.Key, AssetInformationDto> by PersistedJsonSqlDelightStoreBuilder()
    .buildKeyed(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofOutcome(
            mapper = { key ->
                discoveryService.getAssetInformation(assetTicker = key.networkTicker)
            }
        ),
        keySerializer = Key.serializer(),
        dataSerializer = AssetInformationDto.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    KeyedFlushableDataSource<AssetInformationStore.Key> {

    @Serializable
    data class Key(
        val networkTicker: String
    )

    override fun invalidate(param: Key) {
        markAsStale(param)
    }

    override fun invalidate() {
        markStoreAsStale()
    }

    companion object {
        private const val STORE_ID = "AssetInformationStore"
    }
}
