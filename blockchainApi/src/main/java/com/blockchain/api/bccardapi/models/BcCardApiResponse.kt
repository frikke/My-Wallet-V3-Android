package com.blockchain.api.bccardapi.models

import com.blockchain.api.paymentmethods.models.DailyLimits
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductsResponse(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("subtitle")
    val subtitle: String,
    @SerialName("description")
    val description: String,
    @SerialName("fee")
    val fee: Long, // TODO should this be long?
    @SerialName("image")
    val image: String,
    @SerialName("brand")
    val brand: String,
    @SerialName("type")
    val type: String,
    @SerialName("availability")
    val availability: Availability
)

@Serializable
data class Availability(
    @SerialName("available")
    val available: Boolean,
    @SerialName("unavaibleReason")
    val unavailableReason: String? = ""
)