package com.blockchain.nabu.models.responses.tokenresponse

import com.blockchain.nabu.metadata.CredentialMetadata
import com.blockchain.nabu.metadata.NabuAccountCredentialsMetadata
import com.blockchain.nabu.metadata.NabuUserCredentialsMetadata
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

fun NabuOfflineTokenResponse.mapToNabuUserMetadata(): NabuUserCredentialsMetadata =
    NabuUserCredentialsMetadata(this.userId, this.token)

fun NabuOfflineTokenResponse.mapToNabuAccountMetadata(): NabuAccountCredentialsMetadata =
    NabuAccountCredentialsMetadata(this.userId, this.token)

fun CredentialMetadata.mapFromMetadata(): NabuOfflineTokenResponse {
    require(userId != null) {
        "Nabu userId cannot be null"
    }
    require(lifetimeToken != null) {
        "Nabu lifetime token cannot be null"
    }

    // requires ensure these values cannot be null, so double bang is ok here
    return NabuOfflineTokenResponse(userId!!, lifetimeToken!!)
}
