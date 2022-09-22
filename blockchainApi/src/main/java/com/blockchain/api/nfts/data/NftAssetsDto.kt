package com.blockchain.api.nfts.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NftAssetsDto(
    @SerialName("next")
    val next: String? = null,
    @SerialName("previous")
    val previous: String? = null,
    @SerialName("assets")
    val assets: ArrayList<NftAssetDto>,
)

@Serializable
data class NftAssetDto(
    @SerialName("token_id") val tokenId: String?,
    @SerialName("name") val name: String?,
    @SerialName("description") val description: String?,
    @SerialName("image_url") val imageUrl: String?,
    @SerialName("image_preview_url") val imagePreviewUrl: String?,
    @SerialName("traits") val traits: List<NftTraitDto>
)

@Serializable
data class NftTraitDto(
    @SerialName("trait_type") val name: String,
    @SerialName("value") val value: String
)
