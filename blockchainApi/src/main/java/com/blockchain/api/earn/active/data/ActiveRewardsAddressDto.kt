package com.blockchain.api.earn.active.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActiveRewardsAddressDto(
    @SerialName("accountRef")
    val address: String
)
