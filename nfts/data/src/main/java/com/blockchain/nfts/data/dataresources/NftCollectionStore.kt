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
                nftApiService.getNftsForAddress(address = "0x5D70101143BF7bbc889D757613e2B2761bD447EC")
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
