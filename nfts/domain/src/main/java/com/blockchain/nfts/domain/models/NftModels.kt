package com.blockchain.nfts.domain.models

data class NftAsset(
    val tokenId: String,
    val iconUrl: String,
    val nftData: NftData
)

data class NftData(
    val name: String,
    val description: String,
    val attributes: List<String>
)
