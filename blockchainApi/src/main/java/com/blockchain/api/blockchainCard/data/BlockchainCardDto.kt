package com.blockchain.api.blockchainCard.data

import kotlinx.serialization.Serializable

@Serializable
data class ProductsResponse(
    val productCode: String,
    val price: Price,
    val brand: String,
    val type: String
)

@Serializable
data class CardsResponse(
    val id: String,
    val type: String,
    val last4: String,
    val expiry: String,
    val brand: String,
    val status: String,
    val createdAt: String
)

@Serializable
data class Price(
    val symbol: String,
    val value: String,
)

@Serializable
class CardCreationRequestBody(
    val productCode: String,
    val ssn: String
)
