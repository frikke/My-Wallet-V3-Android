package com.blockchain.nfts.data.dataresources

import com.blockchain.api.nfts.data.NftAssetsDto
import com.blockchain.api.services.NftApiService
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource

class NftCollectionStore(
    private val nftApiService: NftApiService
) : Store<NftAssetsDto> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = "NftCollectionStore",
        fetcher = Fetcher.ofSingle(
            mapper = {
                nftApiService.getNftsForAddress(address = "0xe74e48007CB5D0464640b5D760d26f7b4DE6d790")
            }
        ),
        dataSerializer = NftAssetsDto.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }
}




