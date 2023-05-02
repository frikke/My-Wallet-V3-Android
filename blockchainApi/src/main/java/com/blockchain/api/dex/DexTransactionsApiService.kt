package com.blockchain.api.dex

import com.blockchain.api.selfcustody.PreImage
import kotlinx.serialization.SerialName

class DexTransactionsApiService(private val api: DexTxApi) {
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
        network: String,
        amount: String,
    ) = api.buildTx(
        BuildDexTxBodyRequest(
            network = network,
            intent = BuildDexTransactionIntent(
                type = "TOKEN_APPROVAL",
                destination = destination,
                fee = "NORMAL",
                maxVerificationVersion = 1,
                spender = ZEROX_EXCHANGE,
                amount = amount,
                swapTx = null,
                sources = sources
            )
        )
    )

    suspend fun buildDexSwapTx(
        destination: String,
        sources: List<PubKeySource>,
        network: String,
        data: String,
        value: String,
        gasLimit: String
    ) = api.buildTx(
        BuildDexTxBodyRequest(
            network = network,
            intent = BuildDexTransactionIntent(
                type = "SWAP",
                destination = destination,
                fee = "NORMAL",
                maxVerificationVersion = 1,
                spender = ZEROX_EXCHANGE,
                amount = null,
                swapTx = SwapTx(
                    data = data,
                    value = value,
                    gasLimit = gasLimit
                ),
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
data class BuildDexTxBodyRequest(
    val intent: BuildDexTransactionIntent,
    val network: String
)

@kotlinx.serialization.Serializable
data class BuildDexTransactionIntent(
    val type: String,
    val destination: String,
    val sources: List<PubKeySource>,
    val fee: String,
    val maxVerificationVersion: Int,
    val spender: String,
    val amount: String?,
    val swapTx: SwapTx?,
)

@kotlinx.serialization.Serializable
data class SwapTx(
    val data: String,
    val value: String,
    val gasLimit: String,
)

@kotlinx.serialization.Serializable
data class BuiltDexTxResponse(
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
