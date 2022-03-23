package com.blockchain.nabu.models.responses.wallet

import com.squareup.moshi.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RetailJwtResponse(
    @field:Json(name = "success")
    @SerialName("success")
    val isSuccessful: Boolean,
    val token: String?,
    val error: String?
)
