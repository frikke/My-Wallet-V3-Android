package com.blockchain.api.ethereum.layertwo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PushTransactionResponse(
    @SerialName("txHash")
    val hash: String,
    @SerialName("success")
    val success: Boolean
)
