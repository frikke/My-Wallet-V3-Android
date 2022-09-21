package com.blockchain.nfts.data.repository

import com.blockchain.api.nfts.data.NftAssetsResponse
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.nfts.data.dataresources.NftCollectionStore
import com.blockchain.nfts.domain.models.NftAsset
import com.blockchain.nfts.domain.models.NftData
import com.blockchain.nfts.domain.service.NftService
import com.blockchain.store.mapData
import kotlinx.coroutines.flow.Flow

class NftRepository(private val nftCollectionStore: NftCollectionStore) : NftService {

    override suspend fun getNftForAddress(
        freshnessStrategy: FreshnessStrategy,
        network: String,
        address: String
    ): Flow<DataResource<List<NftAsset>>> {
        return nftCollectionStore.stream(freshnessStrategy)
            .mapData {
                it.mapToDomain()
            }
    }

    private fun NftAssetsResponse.mapToDomain(): List<NftAsset> =
        this.assets.map {
            NftAsset(
                it.tokenId.orEmpty(),
                it.imageUrl ?: it.imagePreviewUrl.orEmpty(),
                NftData(
                    it.name.orEmpty(),
                    it.description.orEmpty(),
                    it.traits
                )
            )
        }
}
