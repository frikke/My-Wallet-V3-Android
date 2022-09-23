package com.blockchain.nfts.domain.models

data class NftAsset(
    val id: String,
    val tokenId: String,
    val imageUrl: String,
    val name: String,
    val description: String,
    val contract: NftContract,
    val creator: NftCreator,
    val traits: List<NftTrait>
)

data class NftContract(
    val address: String
)

data class NftCreator(
    val imageUrl: String,
    val name: String,
    val isVerified: Boolean
)

data class NftTrait(
    val name: String,
    val value: String
)
