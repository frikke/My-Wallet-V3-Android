package com.blockchain.api.dex

import com.blockchain.api.selfcustody.SwapTx
import com.blockchain.api.services.DynamicSelfCustodyService

class DexTransactionsApiService(
    private val api: DexTxApi,
    private val dynamicSelfCustodyService: DynamicSelfCustodyService
) {
    suspend fun allowance(
        address: String,
        currencyContract: String,
        networkSymbol: String
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
        networkNativeAssetTicker: String,
        amount: String
    ) = dynamicSelfCustodyService.buildTransaction(
        type = "TOKEN_APPROVAL",
        maxVerificationVersion = 1,
        amount = amount,
        fee = "NORMAL",
        swapTx = null,
        transactionTarget = destination,
        spender = ZEROX_EXCHANGE,
        currency = networkNativeAssetTicker
    )

    suspend fun buildDexSwapTx(
        destination: String,
        networkNativeCurrency: String,
        data: String,
        value: String,
        fee: String,
        gasLimit: String
    ) = dynamicSelfCustodyService.buildTransaction(
        type = "SWAP",
        maxVerificationVersion = 1,
        amount = null,
        fee = fee,
        swapTx = SwapTx(
            data = data,
            value = value,
            gasLimit = gasLimit
        ),
        transactionTarget = destination,
        spender = ZEROX_EXCHANGE,
        currency = networkNativeCurrency,
        feeCurrency = networkNativeCurrency
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
data class GasValuesPayload(
    val payload: Payload?
)

@kotlinx.serialization.Serializable
data class Payload(
    val gasLimit: HexValue?,
    val gasPrice: HexValue?,
)

@kotlinx.serialization.Serializable
data class HexValue(
    val type: String,
    val hex: String
)
