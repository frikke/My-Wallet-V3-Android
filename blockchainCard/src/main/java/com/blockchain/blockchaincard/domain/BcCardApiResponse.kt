package com.blockchain.api.bccardapi.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductsResponse(
    @SerialName("productCode")
    val productCode: String,
    @SerialName("fee")
    val fee: Long, // TODO should this be long?
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