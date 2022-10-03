package com.blockchain.nfts.data.repository

import com.blockchain.api.nfts.data.NftAssetResponse
import com.blockchain.api.services.NftService
import com.blockchain.nfts.domain.models.NftAsset
import com.blockchain.nfts.domain.models.NftData

class NftRepository(private val nftService: NftService) : com.blockchain.nfts.domain.service.NftService {

    override suspend fun getNftForAddress(network: String, address: String): List<NftAsset> =
        nftService.getNftsForAddress(address = address).mapToDomain()

    private fun NftAssetResponse.mapToDomain(): List<NftAsset> =
        this.nftBalances.balances.map {
            NftAsset(
                it.tokenId,
                it.metadata.image,
                NftData(
                    it.metadata.name,
                    it.metadata.description,
                    it.metadata.attributes.map { it.value }
                )
            )
        }
}
