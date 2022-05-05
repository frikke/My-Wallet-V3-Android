package com.blockchain.nabu.metadata

import com.blockchain.serialization.JsonSerializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NabuUserCredentialsMetadata(
    @SerialName("user_id")
    override val userId: String,

    @SerialName("lifetime_token")
    override val lifetimeToken: String,

    @SerialName("exchange_user_id")
    override val exchangeUserId: String? = null,

    @SerialName("exchange_lifetime_token")
    override val exchangeLifetimeToken: String? = null
) : CredentialMetadata {

    companion object {
        const val USER_CREDENTIALS_METADATA_NODE = 10
    }
}

@Serializable
data class NabuAccountCredentialsMetadata(
    @SerialName("nabu_user_id")
    override val userId: String? = null,

    @SerialName("nabu_lifetime_token")
    override val lifetimeToken: String? = null,

    @SerialName("exchange_user_id")
    override val exchangeUserId: String? = null,

    @SerialName("exchange_lifetime_token")
    override val exchangeLifetimeToken: String? = null
) : CredentialMetadata {

    companion object {
        const val ACCOUNT_CREDENTIALS_METADATA_NODE = 13
    }
}

sealed interface CredentialMetadata : JsonSerializable {
    val userId: String?
    val lifetimeToken: String?
    val exchangeUserId: String?
    val exchangeLifetimeToken: String?

    fun isValid() = userId.isNullOrEmpty().not() && lifetimeToken.isNullOrEmpty().not()
}
