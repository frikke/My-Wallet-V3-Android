package com.blockchain.nabu.models.responses.wallet

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RetailJwtResponse(
    @SerialName("success")
    val isSuccessful: Boolean,
    val token: String?,
    val error: String?
)
