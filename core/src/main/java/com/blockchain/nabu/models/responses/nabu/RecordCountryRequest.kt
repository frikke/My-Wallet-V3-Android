package com.blockchain.nabu.models.responses.nabu

import kotlinx.serialization.Serializable

@Serializable
internal data class RecordCountryRequest(
    val jwt: String,
    val countryCode: String,
    val notifyWhenAvailable: Boolean,
    val state: String? = null
)
