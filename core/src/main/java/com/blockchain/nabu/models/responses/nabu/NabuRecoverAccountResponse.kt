package com.blockchain.nabu.models.responses.nabu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Field

@Serializable
data class NabuRecoverAccountResponse(
    @Field("token")
    @SerialName("token")
    val token: String
)
