package com.blockchain.api.eligibility.data

import kotlinx.serialization.Serializable

@Serializable
data class StateResponse(
    val code: String,
    val name: String,
    val scopes: List<String>,
    val countryCode: String
)
