package com.blockchain.api.nabu.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddressRequest(
    val line1: String?,
    val line2: String?,
    val city: String?,
    val state: String?,
    val postCode: String,
    @SerialName("country")
    val countryCode: String?
)
