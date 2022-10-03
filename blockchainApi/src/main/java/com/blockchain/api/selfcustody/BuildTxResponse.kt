package com.blockchain.api.selfcustody

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class BuildTxResponse(
    @SerialName("summary")
    val summary: TransactionSummary,
    @SerialName("rawTx")
    val rawTx: JsonObject,
    @SerialName("preImages")
    val preImages: List<PreImage>
)

@Serializable
data class TransactionSummary(
    @SerialName("relativeFee")
    val relativeFee: String,
    @SerialName("absoluteFeeMaximum")
    val maxFee: String,
    @SerialName("absoluteFeeEstimate")
    val absoluteFeeEstimate: String,
    @SerialName("amount")
    val amount: String,
    @SerialName("balance")
    val balance: String
)

@Serializable
data class RawTransaction(
    val version: Int,
    val created: Long,
    val ttl: Int
)

@Serializable
data class PreImage(
    @SerialName("preImage")
    val rawPreImage: String,
    @SerialName("signingKey")
    val signingKey: String,
    @SerialName("signatureAlgorithm")
    val signatureAlgorithm: SignatureAlgorithm,
    @SerialName("descriptor")
    val descriptor: String?
)

@Serializable
enum class SignatureAlgorithm {
    @SerialName("secp256k1")
    SECP256K1,
    UNKNOWN
}
