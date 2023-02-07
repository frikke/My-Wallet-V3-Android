package com.blockchain.api.earn.passive.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InterestAvailableTickersDto(
    @SerialName("instruments")
    val networkTickers: List<String>
)
