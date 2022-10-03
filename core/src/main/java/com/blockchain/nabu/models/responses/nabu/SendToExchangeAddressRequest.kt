package com.blockchain.nabu.models.responses.nabu

import kotlinx.serialization.Serializable

@Serializable
data class SendToExchangeAddressRequest(
    val currency: String
)

@Serializable
data class SendToExchangeAddressResponse(
    val address: String,
    val currency: String,
    val state: State // "PENDING" | "ACTIVE" | "BLOCKED"
)

@Serializable
enum class State {
    PENDING, ACTIVE, BLOCKED
}
