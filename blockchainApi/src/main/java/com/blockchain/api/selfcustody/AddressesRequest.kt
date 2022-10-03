package com.blockchain.api.selfcustody

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddressesRequest(
    @SerialName("auth")
    val auth: AuthInfo,
    @SerialName("currencies")
    val currencies: List<CurrencyAddressInfo>
)

@Serializable
data class CurrencyAddressInfo(
    @SerialName("ticker")
    val ticker: String,
    @SerialName("memo")
    val memo: String? = null
)
