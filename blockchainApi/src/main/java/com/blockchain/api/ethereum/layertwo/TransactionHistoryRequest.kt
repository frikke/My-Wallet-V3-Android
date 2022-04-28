package com.blockchain.api.ethereum.layertwo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionHistoryRequest(
    @SerialName("address")
    val address: String,
    @SerialName("apiCode")
    val apiCode: String,
    @SerialName("network")
    val network: String,
    @SerialName("identifier")
    val tickerId: String
)
