package com.blockchain.api.earn.staking.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StakingAddressDto(
    @SerialName("accountRef")
    val address: String
)
