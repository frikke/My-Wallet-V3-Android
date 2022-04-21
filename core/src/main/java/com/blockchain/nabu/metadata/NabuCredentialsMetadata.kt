package com.blockchain.nabu.metadata

import com.blockchain.serialization.JsonSerializable
import com.squareup.moshi.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NabuCredentialsMetadata(
    @field:Json(name = "user_id")
    @SerialName("user_id")
    val userId: String,

    @field:Json(name = "lifetime_token")
    @SerialName("lifetime_token")
    val lifetimeToken: String,

    @field:Json(name = "exchange_user_id")
    @SerialName("exchange_user_id")
    val exchangeUserId: String?,

    @field:Json(name = "exchange_lifetime_token")
    @SerialName("exchange_lifetime_token")
    val exchangeLifetimeToken: String?
) : JsonSerializable {

    fun isValid() = userId.isNotEmpty() && lifetimeToken.isNotEmpty()

    companion object {
        const val USER_CREDENTIALS_METADATA_NODE = 10
    }
}
