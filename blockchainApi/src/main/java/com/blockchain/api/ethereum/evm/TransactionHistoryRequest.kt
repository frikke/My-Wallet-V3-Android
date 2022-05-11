package com.blockchain.api.ethereum.evm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionHistoryRequest(
    @SerialName("addresses")
    val addresses: List<String>,
    @SerialName("apiCode")
    val apiCode: String,
    @SerialName("network")
    val network: String,
    @SerialName("identifier")
    val contractAddress: String?
)
