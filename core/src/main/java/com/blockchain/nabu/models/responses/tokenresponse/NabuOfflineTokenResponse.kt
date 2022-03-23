package com.blockchain.nabu.models.responses.tokenresponse

import com.blockchain.nabu.metadata.NabuCredentialsMetadata
import kotlinx.serialization.Serializable

@Serializable
data class NabuOfflineTokenRequest(
    val jwt: String
)

@Serializable
data class NabuOfflineTokenResponse(
    val userId: String,
    val token: String
)

fun NabuOfflineTokenResponse.mapToMetadata(): NabuCredentialsMetadata =
    NabuCredentialsMetadata(this.userId, this.token)

fun NabuCredentialsMetadata.mapFromMetadata(): NabuOfflineTokenResponse =
    NabuOfflineTokenResponse(this.userId, this.lifetimeToken)
