package com.blockchain.api.selfcustody

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionHistoryRequest(
    @SerialName("auth")
    val auth: AuthInfo,
    @SerialName("currency")
    val currency: String,
    @SerialName("identifier")
    val contractAddress: String?
)
