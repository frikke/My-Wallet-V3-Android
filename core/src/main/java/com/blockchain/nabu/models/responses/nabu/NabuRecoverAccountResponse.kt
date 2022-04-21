package com.blockchain.nabu.models.responses.nabu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Field

@Serializable
data class NabuRecoverAccountResponse(
    @Field("token")
    @SerialName("token")
    val token: String,

    @Field("userCredentialsId")
    @SerialName("userCredentialsId")
    val userCredentialsId: String?,

    @Field("mercuryLifetimeToken")
    @SerialName("mercuryLifetimeToken")
    val mercuryLifetimeToken: String?
)
