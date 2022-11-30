package com.blockchain.api.interest.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InterestRateDto(
    val rate: Double
)

@Serializable
data class InterestRatesDto(
    @SerialName("rates")
    val rates: Map<String, InterestTokenRateDto>
)

@Serializable
data class InterestTokenRateDto(
    @SerialName("rate")
    val rate: Double
)
