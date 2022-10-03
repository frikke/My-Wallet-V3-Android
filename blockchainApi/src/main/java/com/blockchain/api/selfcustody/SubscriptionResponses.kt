package com.blockchain.api.selfcustody

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetSubscriptionsResponse(
    @SerialName("currencies")
    val currencies: List<CurrencyInfo>
)

@Serializable
data class CurrencyInfo(
    @SerialName("ticker")
    val ticker: String
)
