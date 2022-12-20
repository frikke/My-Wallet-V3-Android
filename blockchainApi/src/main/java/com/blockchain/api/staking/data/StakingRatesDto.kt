package com.blockchain.api.staking.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StakingRatesDto(
    @SerialName("rates")
    val rates: Map<String, StakingTokenRateDto>
)

@Serializable
data class StakingTokenRateDto(
    @SerialName("rate")
    val rate: Double,
    @SerialName("commission")
    val commission: Double
)
