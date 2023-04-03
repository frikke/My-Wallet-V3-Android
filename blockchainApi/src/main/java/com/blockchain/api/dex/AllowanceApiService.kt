package com.blockchain.api.dex

import com.blockchain.api.selfcustody.PreImage
import kotlinx.serialization.SerialName

class AllowanceApiService(private val api: AllowanceApi) {
    suspend fun allowance(
        address: String,
        currencyContract: String,
        networkSymbol: String,
    ) = api.allowance(
        AllowanceBodyRequest(
            addressOwner = address,
            spender = ZEROX_EXCHANGE,
            currency = currencyContract,
            network = networkSymbol
        )
    )

    suspend fun buildAllowanceTx(
        destination: String,
        sources: List<PubKeySource>,
        network: String
    ) = api.buildTx(
        BuildAllowanceTxBodyRequest(
            network = network,
            intent = BuildAllowanceIntent(
                type = "TOKEN_APPROVAL",
                destination = destination,
                fee = "NORMAL",
                maxVerificationVersion = 1,
                spender = ZEROX_EXCHANGE,
                amount = "MAX",
                sources = sources
            )
        )
    )
}

private const val ZEROX_EXCHANGE = "ZEROX_EXCHANGE"

@kotlinx.serialization.Serializable
data class AllowanceBodyRequest(
    val addressOwner: String,
    val spender: String,
    val currency: String,
    val network: String
)

@kotlinx.serialization.Serializable
data class TokenAllowanceResponse(
    val result: AllowanceResult
)

@kotlinx.serialization.Serializable
data class AllowanceResult(
    val allowance: String
)

@kotlinx.serialization.Serializable
data class PubKeySource(
    val pubKey: String,
    val style: String,
    val descriptor: String
)

@kotlinx.serialization.Serializable
data class BuildAllowanceTxBodyRequest(
    val intent: BuildAllowanceIntent,
    val network: String
)

@kotlinx.serialization.Serializable
data class BuildAllowanceIntent(
    val type: String,
    val destination: String,
    val sources: List<PubKeySource>,
    val fee: String,
    val maxVerificationVersion: Int,
    val spender: String,
    val amount: String
)

@kotlinx.serialization.Serializable
data class BuildAllowanceTxResponse(
    @SerialName("rawTx")
    val rawTx: RawTxResponse,
    @SerialName("preImages")
    val preImages: List<PreImage>
)

@kotlinx.serialization.Serializable
data class RawTxResponse(
    val version: Int,
    val payload: Payload
)

@kotlinx.serialization.Serializable
data class Payload(
    val to: String,
    val nonce: Int,
    val value: HexValue,
    val gasLimit: HexValue,
    val gasPrice: HexValue,
    val chainId: Int,
    val data: String
)
@kotlinx.serialization.Serializable
data class HexValue(
    val type: String,
    val hex: String
)
