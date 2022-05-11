package com.blockchain.api.ethereum.evm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PushTransactionResponse(
    @SerialName("txId")
    val txId: String,
    @SerialName("success")
    val success: Boolean
)
