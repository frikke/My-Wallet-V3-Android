package com.blockchain.nabu.models.responses.nabu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NabuRecoverAccountResponse(
    @SerialName("token")
    val token: String,
    @SerialName("userCredentialsId")
    val userCredentialsId: String?,
    @SerialName("mercuryLifetimeToken")
    val mercuryLifetimeToken: String?
)
