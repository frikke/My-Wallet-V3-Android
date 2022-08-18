package com.blockchain.nfts.data.repository

import com.blockchain.api.nfts.data.NftAssetsResponse
import com.blockchain.api.services.NftService
import com.blockchain.nfts.domain.models.NftAsset
import com.blockchain.nfts.domain.models.NftData

class NftRepository(private val nftService: NftService) : com.blockchain.nfts.domain.service.NftService {

    override suspend fun getNftForAddress(network: String, address: String): List<NftAsset> =
        nftService.getNftsForAddress(address = address).mapToDomain()

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
