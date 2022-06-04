package com.blockchain.api.nftwaitlist.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class NftWaitlistDto private constructor(
    @SerialName("email") val email: String,
    @SerialName("feature") val feature: String = "mobile_view_nft_support",
    @SerialName("user_data") val userData: UserDataDto = UserDataDto
) {
    companion object {
        fun build(email: String): NftWaitlistDto {
            return NftWaitlistDto(email = email)
        }
    }
}

@Serializable
object UserDataDto
