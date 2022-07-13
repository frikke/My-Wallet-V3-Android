package com.blockchain.api.selfcustody

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class PushTxRequest(
    @SerialName("auth")
    val auth: AuthInfo,
    @SerialName("currency")
    val currency: String,
    @SerialName("rawTx")
    val rawTx: JsonObject,
    @SerialName("signatures")
    val signatures: List<Signature>
)

@Serializable
data class Signature(
    @SerialName("preImage")
    val preImage: String,
    @SerialName("signingKey")
    val signingKey: String,
    @SerialName("signatureAlgorithm")
    val signatureAlgorithm: SignatureAlgorithm,
    @SerialName("signature")
    val signature: String
)
