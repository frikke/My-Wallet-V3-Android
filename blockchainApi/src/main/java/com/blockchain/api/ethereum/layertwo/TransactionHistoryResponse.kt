package com.blockchain.api.ethereum.layertwo

import java.math.BigInteger
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionHistoryResponse(
    @SerialName("address")
    val address: String,
    @SerialName("history")
    val history: List<L2TransactionResponse>
)

@Serializable
data class L2TransactionResponse(
    @SerialName("txId")
    val id: String,
    @SerialName("status")
    val status: Status,
    @SerialName("timestamp")
    val timeStamp: Long,
    @SerialName("fee")
    val fee: @Contextual BigInteger
)

enum class Status {
    PENDING,
    CONFIRMING,
    COMPLETED,
    FAILED
}
