package com.blockchain.api.selfcustody

import java.math.BigInteger
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionHistoryResponse(
    val history: List<TransactionResponse>
)

@Serializable
data class TransactionResponse(
    val txId: String,
    val status: Status?,
    val timestamp: Long,
    val fee: String,
    val movements: List<TransactionMovement>
)

@Serializable
data class TransactionMovement(
    @SerialName("type")
    val type: TransactionDirection?,
    @SerialName("address")
    val address: String,
    @SerialName("amount")
    val amount: @Contextual BigInteger,
    @SerialName("identifier")
    val contractAddress: String
)

enum class TransactionDirection {
    SENT,
    RECEIVED
}

enum class Status {
    PENDING,
    CONFIRMING,
    COMPLETED,
    FAILED
}
