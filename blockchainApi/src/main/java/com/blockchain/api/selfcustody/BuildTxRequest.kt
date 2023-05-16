package com.blockchain.api.selfcustody

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BuildTxRequest(
    @SerialName("auth")
    val auth: AuthInfo,
    @SerialName("currency")
    val currency: String,
    @SerialName("account")
    val accountIndex: Int,
    @SerialName("type")
    val type: String,
    @SerialName("destination")
    val destination: String,
    @SerialName("amount")
    val amount: String?,
    @SerialName("fee")
    val fee: String,
    @SerialName("extraData")
    val extraData: ExtraData,
    @SerialName("maxVerificationVersion")
    val maxVerificationVersion: Int,
)

@Serializable
data class SwapTx(
    val data: String,
    val value: String,
    val gasLimit: String
)

@Serializable
data class ExtraData(
    @SerialName("memo")
    val memo: String,
    @SerialName("feeCurrency")
    val feeCurrency: String,
    val swapTx: SwapTx?,
    val spender: String?,
)
