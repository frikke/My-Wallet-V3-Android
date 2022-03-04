package com.blockchain.api.bccardapi.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductsResponse(
    @SerialName("productCode")
    val productCode: String,
    @SerialName("price")
    val price: Price,
    @SerialName("brand")
    val brand: String,
    @SerialName("type")
    val type: String
)

@Serializable
data class Availability(
    @SerialName("available")
    val available: Boolean,
    @SerialName("unavaibleReason")
    val unavailableReason: String? = ""
)

@Serializable
data class Price(
    @SerialName("symbol")
    val symbol: String,
    @SerialName("value")
    val value: String,
)