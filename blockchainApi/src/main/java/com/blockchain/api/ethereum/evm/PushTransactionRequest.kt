package com.blockchain.api.ethereum.evm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PushTransactionRequest(
    @SerialName("rawTx")
    val rawTransaction: String,
    @SerialName("network")
    val networkName: String,
    @SerialName("apiCode")
    val apiCode: String
)
