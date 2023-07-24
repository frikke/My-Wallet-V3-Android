package com.blockchain.nabu.models.responses.tokenresponse

import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class NabuSessionTokenResponse(
    val id: String,
    val userId: String,
    val token: String,
    val isActive: Boolean,
    val expiresAt: String,
    val insertedAt: String,
    val updatedAt: String? = ""
) {
    val authHeader
        get() = "Bearer $token"

    fun hasExpired(): Boolean {
        return try {
            val dateTimeInstant = Instant.parse(expiresAt)
            val currentInstant = Instant.now()
            dateTimeInstant.isBefore(currentInstant)
        } catch (e: Exception) {
            true
        }
    }
}
