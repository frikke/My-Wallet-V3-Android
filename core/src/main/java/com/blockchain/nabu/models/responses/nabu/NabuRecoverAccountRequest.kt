package com.blockchain.nabu.models.responses.nabu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Field

@Serializable
data class NabuRecoverAccountRequest(
    val jwt: String,
    @Field("recoveryToken")
    @SerialName("recoveryToken")
    val recoveryToken: String
)
