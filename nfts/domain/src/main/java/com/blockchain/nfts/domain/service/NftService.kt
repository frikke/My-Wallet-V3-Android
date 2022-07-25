package com.blockchain.nfts.domain.service

import com.blockchain.nfts.domain.models.NftAsset

interface NftService {
    suspend fun getNftForAddress(network: String = "ETH", address: String): List<NftAsset>
}
