package com.blockchain.coincore.loader

import com.blockchain.api.services.AssetDiscoveryApiService
import com.blockchain.api.services.DynamicAsset
import com.blockchain.api.services.DynamicAssetList
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.KeyedFlushableDataSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

class NonCustodialL2sDynamicAssetStore(
    private val discoveryService: AssetDiscoveryApiService
) : KeyedStore<NonCustodialL2sDynamicAssetStore.Key, DynamicAssetList> by PersistedJsonSqlDelightStoreBuilder()
    .buildKeyed(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofOutcome(
            mapper = { key ->
                discoveryService.getL2AssetsForEVM(key.networkTickers)
            }
        ),
        keySerializer = Key.serializer(),
        dataSerializer = ListSerializer(DynamicAsset.serializer()),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    KeyedFlushableDataSource<NonCustodialL2sDynamicAssetStore.Key> {

    @Serializable
    data class Key(
        val networkTickers: List<String>
    )

    override fun invalidate(param: Key) {
        markAsStale(param)
    }

    override fun invalidate() {
        markStoreAsStale()
    }

    companion object {
        private const val STORE_ID = "NonCustodialL2sDynamicAssetStore"
    }
}
