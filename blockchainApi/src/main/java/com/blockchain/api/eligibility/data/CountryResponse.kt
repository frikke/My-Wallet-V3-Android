package com.blockchain.api.eligibility.data

import kotlinx.serialization.Serializable

@Serializable
data class CountryResponse(
    val code: String,
    val name: String,
    val states: List<String>,
    val scopes: List<String>,
    val regions: List<String>
)
