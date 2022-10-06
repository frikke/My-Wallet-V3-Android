package com.blockchain.nfts.data.repository

import com.blockchain.api.nfts.data.NftAssetsDto
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.nfts.data.dataresources.NftCollectionStore
import com.blockchain.nfts.domain.models.NftAsset
import com.blockchain.nfts.domain.models.NftAssetsPage
import com.blockchain.nfts.domain.models.NftContract
import com.blockchain.nfts.domain.models.NftCreator
import com.blockchain.nfts.domain.models.NftTrait
import com.blockchain.nfts.domain.service.NftService
import com.blockchain.store.mapData
import kotlinx.coroutines.flow.Flow

class NftRepository(private val nftCollectionStore: NftCollectionStore) : NftService {

    override suspend fun getNftCollectionForAddress(
        freshnessStrategy: FreshnessStrategy,
        address: String,
        pageKey: String?
    ): Flow<DataResource<NftAssetsPage>> {
        return nftCollectionStore.stream(
            freshnessStrategy.withKey(
                NftCollectionStore.Key(address = address, pageKey = pageKey)
            )
        ).mapData {
            it.mapToDomain()
        }
    }

    override suspend fun getNftAsset(
        freshnessStrategy: FreshnessStrategy,
        address: String,
        nftId: String,
        pageKey: String?
    ): Flow<DataResource<NftAsset?>> {
        return getNftCollectionForAddress(
            freshnessStrategy = freshnessStrategy,
            address = address,
            pageKey = pageKey
        ).mapData {
            it.assets.firstOrNull { it.id == nftId }
        }
    }

    private fun NftAssetsDto.mapToDomain(): NftAssetsPage = run {
        NftAssetsPage(
            assets = this.assets.filterNot { it.imageUrl.isNullOrBlank() }
                .map { nftAsset ->
                    NftAsset(
                        id = nftAsset.id.orEmpty(),
                        tokenId = nftAsset.tokenId.orEmpty(),
                        imageUrl = nftAsset.imageUrl ?: nftAsset.imagePreviewUrl.orEmpty(),
                        name = nftAsset.name.orEmpty(),
                        description = nftAsset.description.orEmpty(),
                        contract = NftContract(nftAsset.contract.address),
                        creator = NftCreator(
                            imageUrl = nftAsset.creator.imageUrl,
                            name = nftAsset.creator.address.let {
                                if (it.lowercase().startsWith("0x")) {
                                    it.drop(2).substring(0..5)
                                } else {
                                    it
                                }
                            },
                            isVerified = nftAsset.creator.isVerified
                        ),
                        traits = nftAsset.traits.map { nftTrait ->
                            NftTrait(
                                name = nftTrait.name,
                                value = nftTrait.value
                            )
                        }
                    )
                },
            nextPageKey = this.next
        )
    }
}
