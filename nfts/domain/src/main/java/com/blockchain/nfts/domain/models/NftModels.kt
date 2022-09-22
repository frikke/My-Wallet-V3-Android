package com.blockchain.nfts.domain.models

data class NftAsset(
    val id: String,
    val iconUrl: String,
    val nftData: NftData
)

data class NftData(
    val name: String,
    val description: String,
    val traits: List<NftTrait>
)

data class NftTrait(
    val name: String,
    val value: String
)
