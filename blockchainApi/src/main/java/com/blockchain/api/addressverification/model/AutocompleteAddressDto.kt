package com.blockchain.api.addressverification.model

import kotlinx.serialization.Serializable

@Serializable
data class AutocompleteAddressDto(
    val id: String,
    val type: String,
    val text: String,
    val highlight: String, // 0-2,6-4;0-3
    val description: String,
)
