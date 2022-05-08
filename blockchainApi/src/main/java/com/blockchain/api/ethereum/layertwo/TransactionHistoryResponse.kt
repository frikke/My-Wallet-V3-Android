package com.blockchain.api.ethereum.layertwo

import java.math.BigInteger
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionHistoryResponse(
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
    val fee: @Contextual BigInteger,
    @SerialName("extraData")
    val extraData: L2TransactionData,
    @SerialName("movements")
    val movements: List<TransactionMovement>
)

@Serializable
data class L2TransactionData(
    @SerialName("gasPrice")
    val gasPrice: @Contextual BigInteger,
    @SerialName("gasLimit")
    val gasLimit: @Contextual BigInteger,
    @SerialName("gasUsed")
    val gasUsed: @Contextual BigInteger,
    @SerialName("blockNumber")
    val blockNumber: @Contextual BigInteger
)

@Serializable
data class TransactionMovement(
    @SerialName("type")
    val type: TransactionDirection,
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
