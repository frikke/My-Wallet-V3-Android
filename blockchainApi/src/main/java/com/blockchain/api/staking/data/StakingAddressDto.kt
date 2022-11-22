package com.blockchain.api.staking.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StakingAddressDto(
    @SerialName("accountRef")
    val address: String
)
