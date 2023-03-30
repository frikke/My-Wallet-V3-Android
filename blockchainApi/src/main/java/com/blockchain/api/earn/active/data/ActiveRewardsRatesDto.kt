package com.blockchain.api.earn.active.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActiveRewardsRatesDto(
    @SerialName("rates")
    val rates: Map<String, ActiveRewardsTokenRateDto>
)

@Serializable
data class ActiveRewardsTokenRateDto(
    @SerialName("rate")
    val rate: Double,

    @SerialName("commission")
    val commission: Double,

    @SerialName("triggerPrice")
    val triggerPrice: String?,
)
