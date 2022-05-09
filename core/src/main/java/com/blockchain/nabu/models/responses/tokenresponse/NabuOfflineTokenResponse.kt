package com.blockchain.nabu.models.responses.tokenresponse

import com.blockchain.nabu.metadata.BlockchainAccountCredentialsMetadata
import com.blockchain.nabu.metadata.CredentialMetadata
import com.blockchain.nabu.metadata.NabuLegacyCredentialsMetadata
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

fun NabuOfflineTokenResponse.mapToLegacyCredentials(): NabuLegacyCredentialsMetadata =
    NabuLegacyCredentialsMetadata(userId = this.userId, lifetimeToken = this.token)

fun NabuOfflineTokenResponse.mapToBlockchainCredentialsMetadata(): BlockchainAccountCredentialsMetadata =
    BlockchainAccountCredentialsMetadata(userId = this.userId, lifetimeToken = this.token)

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
