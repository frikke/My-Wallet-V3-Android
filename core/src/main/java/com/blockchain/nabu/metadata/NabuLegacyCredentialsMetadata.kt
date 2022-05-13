package com.blockchain.nabu.metadata

import com.blockchain.serialization.JsonSerializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NabuLegacyCredentialsMetadata(
    @SerialName("user_id")
    override val userId: String,

    @SerialName("lifetime_token")
    override val lifetimeToken: String,
) : CredentialMetadata

@Serializable
data class BlockchainAccountCredentialsMetadata(
    @SerialName("nabu_user_id")
    override val userId: String? = null,

    @SerialName("nabu_lifetime_token")
    override val lifetimeToken: String? = null,

    @SerialName("exchange_user_id")
    val exchangeUserId: String? = null,

    @SerialName("exchange_lifetime_token")
    val exchangeLifetimeToken: String? = null
) : CredentialMetadata {

    companion object {
        fun invalid(): BlockchainAccountCredentialsMetadata = BlockchainAccountCredentialsMetadata()
    }
}

sealed interface CredentialMetadata : JsonSerializable {
    val userId: String?
    val lifetimeToken: String?

    fun isValid() = userId.isNullOrEmpty().not() && lifetimeToken.isNullOrEmpty().not()
}
