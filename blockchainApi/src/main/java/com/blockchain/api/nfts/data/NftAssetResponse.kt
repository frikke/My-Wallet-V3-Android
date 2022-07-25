package com.blockchain.api.nfts.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NftAssetResponse(
    val nftBalances: NftBalances
)

@Serializable
data class NftBalances(
    val address: String,
    val balances: List<NftAssetInfo>,
    val totalCount: Int
)

@Serializable
data class NftAssetInfo(
    @SerialName("contract")
    val contractData: NftContract,
    val tokenId: String,
    val metadata: NftMetadata
)

@Serializable
data class NftContract(
    val name: String,
    val identifier: String,
    val baseUri: String,
    val network: String,
    @SerialName("interface")
    val tokenType: String
)

@Serializable
data class NftMetadata(
    val name: String,
    val image: String,
    val description: String,
    val attributes: List<NftAttribute>,
    val media: List<NftMediaInfo>
)

@Serializable
data class NftAttribute(
    val value: String,
    @SerialName("trait_type")
    val traitType: String
)

@Serializable
data class NftMediaInfo(
    val raw: String,
    val gateway: String?,
    val thumbnail: String?
)
