package com.blockchain.api.services

import com.blockchain.api.nfts.api.NftApi
import com.blockchain.api.nfts.data.NftAssetRequestBody

class NftService internal constructor(
    private val nftApi: NftApi
) {
    suspend fun getNftsForAddress(network: String = "ETH", address: String) =
        nftApi.getAssetsForAddress(
            requestBody = NftAssetRequestBody(
                network = network,
                address = address
            )
        )
}
