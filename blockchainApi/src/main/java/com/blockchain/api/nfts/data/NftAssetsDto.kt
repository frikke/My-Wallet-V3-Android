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
    val assets: ArrayList<NftAssetDto>
)

@Serializable
data class NftAssetDto(
    @SerialName("id") val id: String?,
    @SerialName("token_id") val tokenId: String?,
    @SerialName("name") val name: String?,
    @SerialName("description") val description: String?,
    @SerialName("image_url") val imageUrl: String?,
    @SerialName("image_preview_url") val imagePreviewUrl: String?,
    @SerialName("asset_contract") val contract: NftContractDto,
    @SerialName("creator") val creator: NftCreatorDto,
    @SerialName("traits") val traits: List<NftTraitDto>
)

@Serializable
data class NftContractDto(
    @SerialName("address") val address: String
)

@Serializable
data class NftCreatorDto(
    @SerialName("address") val address: String,
    @SerialName("config") val config: String,
    @SerialName("profile_img_url") val imageUrl: String
) {
    val isVerified: Boolean
        get() = config.lowercase() == VERIFIED

    companion object {
        private const val VERIFIED = "verified"
    }
}

@Serializable
data class NftTraitDto(
    @SerialName("trait_type") val name: String,
    @SerialName("value") val value: String
)
