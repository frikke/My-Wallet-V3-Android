package com.blockchain.api.addressverification.model

import kotlinx.serialization.Serializable

@Serializable
data class AutocompleteAddressResponse(
    val addresses: List<AutocompleteAddressDto>
)
