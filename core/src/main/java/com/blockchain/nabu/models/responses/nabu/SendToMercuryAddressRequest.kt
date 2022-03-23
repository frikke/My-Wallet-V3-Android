package com.blockchain.nabu.models.responses.nabu

import kotlinx.serialization.Serializable

@Serializable
data class SendToMercuryAddressRequest(
    val currency: String
)

@Serializable
data class SendToMercuryAddressResponse(
    val address: String,
    val currency: String,
    val state: State // "PENDING" | "ACTIVE" | "BLOCKED"
)

@Serializable
enum class State {
    PENDING, ACTIVE, BLOCKED
}
