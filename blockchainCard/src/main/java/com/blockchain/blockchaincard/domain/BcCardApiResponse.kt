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
data class CardsResponse(
    @SerialName("cardId")
    val cardId: String,

    @SerialName("type")
    val type: String,

    @SerialName("last4")
    val last4: String,

    @SerialName("expiry")
    val expiry: String,

    @SerialName("brand")
    val brand: String,

    @SerialName("cardStatus")
    val cardStatus: String,

    @SerialName("createdAt")
    val createdAt: String
)

@Serializable
data class Price(
    @SerialName("symbol")
    val symbol: String,
    @SerialName("value")
    val value: String,
)
