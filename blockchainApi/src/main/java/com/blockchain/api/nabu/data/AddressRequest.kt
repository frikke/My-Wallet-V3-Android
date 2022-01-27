package com.blockchain.api.nabu.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddressRequest(
    @SerialName("line1")
    val line1: String?,
    @SerialName("line2")
    val line2: String?,
    @SerialName("city")
    val city: String?,
    @SerialName("state")
    val state: String?,
    @SerialName("postCode")
    val postCode: String,
    @SerialName("country")
    val countryCode: String?
)
