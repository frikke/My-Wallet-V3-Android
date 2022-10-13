package com.blockchain.api.selfcustody

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BalancesRequest(
    @SerialName("auth")
    val auth: AuthInfo,
    @SerialName("currencies")
    val currencies: List<CurrencyInfo>?,
    @SerialName("fiatCurrency")
    val fiatCurrency: String
)
