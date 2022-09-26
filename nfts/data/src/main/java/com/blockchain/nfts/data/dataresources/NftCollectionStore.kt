package com.blockchain.nfts.data.dataresources

import com.blockchain.api.nfts.data.NftAssetsDto
import com.blockchain.api.services.NftApiService
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.KeyedFlushableDataSource
import kotlinx.serialization.Serializable

class NftCollectionStore(
    private val nftApiService: NftApiService
) : KeyedStore<NftCollectionStore.Key, NftAssetsDto> by PersistedJsonSqlDelightStoreBuilder()
    .buildKeyed(
        storeId = "NftCollectionStore",
        fetcher = Fetcher.Keyed.ofSingle(
            mapper = { key ->
                nftApiService.getNftCollection(
                    address = key.address
                )
            }
        ),
        keySerializer = Key.serializer(),
        dataSerializer = NftAssetsDto.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    KeyedFlushableDataSource<NftCollectionStore.Key> {

    @Serializable
    data class Key(
        val address: String,
    )

    override fun invalidate(param: Key) {
        markAsStale(param)
    }

    override fun invalidate() {
        markStoreAsStale()
    }
}
