package com.blockchain.coincore.loader

import com.blockchain.api.services.AssetDiscoveryApiService
import com.blockchain.api.services.DynamicAsset
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import kotlinx.serialization.builtins.ListSerializer

class EthErc20sStore(private val discoveryService: AssetDiscoveryApiService) :
    Store<List<DynamicAsset>> by PersistedJsonSqlDelightStoreBuilder().build(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofSingle {
            discoveryService.getEthErc20s()
        },
        dataSerializer = ListSerializer(DynamicAsset.serializer()),
        mediator = FreshnessMediator(Freshness.ofHours(24 * 3))
    ) {
    companion object {
        private const val STORE_ID = "EthErc20sStore"
    }
}

class OtherNetworksErc20sStore(private val discoveryService: AssetDiscoveryApiService) :
    Store<List<DynamicAsset>> by PersistedJsonSqlDelightStoreBuilder().build(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofSingle {
            discoveryService.getOtherNetworksErc20s()
        },
        dataSerializer = ListSerializer(DynamicAsset.serializer()),
        mediator = FreshnessMediator(Freshness.ofHours(24 * 3))
    ) {
    companion object {
        private const val STORE_ID = "OtherNetworksErc20sStore"
    }
}
