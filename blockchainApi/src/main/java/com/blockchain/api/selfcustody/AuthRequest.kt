package com.blockchain.api.selfcustody

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    @SerialName("guid")
    val guid: String,
    @SerialName("sharedKeyHash")
    val sharedKey: String
)

@Serializable
data class CommonResponse(
    @SerialName("success")
    val success: Boolean
)
