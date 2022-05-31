package com.blockchain.nabu.models.responses.tokenresponse

import com.blockchain.nabu.metadata.BlockchainAccountCredentialsMetadata
import com.blockchain.nabu.metadata.CredentialMetadata
import com.blockchain.nabu.metadata.NabuLegacyCredentialsMetadata
import kotlinx.serialization.Serializable

@Serializable
data class NabuOfflineTokenRequest(
    val jwt: String
)

/**
 * should only used as response from POST /users
 * This is a get or create endpoint. Created specifies if user was created.
 * The reason that we handle the created is because under some racing conditions,
 * this POST can be called multiple times.
 */
@Serializable
data class NabuOfflineTokenResponse(
    val userId: String,
    val token: String,
    val created: Boolean
)

data class NabuOfflineToken(
    val userId: String,
    val token: String
) {

    fun isValid(): Boolean = userId.isNotEmpty() && token.isNotEmpty()
}

fun NabuOfflineTokenResponse.toNabuOfflineToken() = NabuOfflineToken(
    userId = userId,
    token = token
)

fun NabuOfflineToken.mapToLegacyCredentials(): NabuLegacyCredentialsMetadata =
    NabuLegacyCredentialsMetadata(userId = this.userId, lifetimeToken = this.token)

fun NabuOfflineToken.mapToBlockchainCredentialsMetadata(): BlockchainAccountCredentialsMetadata =
    BlockchainAccountCredentialsMetadata(userId = this.userId, lifetimeToken = this.token)

fun CredentialMetadata.toNabuOfflineToken(): NabuOfflineToken {
    require(userId != null) {
        "Nabu userId cannot be null"
    }
    require(lifetimeToken != null) {
        "Nabu lifetime token cannot be null"
    }

    // requires ensure these values cannot be null, so double bang is ok here
    return NabuOfflineToken(userId!!, lifetimeToken!!)
}
