package com.blockchain.nabu.models.responses.nabu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Field

@Serializable
data class NabuRecoverAccountRequest(
    val jwt: String,
    @Field("recovery_token")
    @SerialName("recovery_token")
    val recoveryToken: String
)
