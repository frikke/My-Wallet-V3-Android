package com.blockchain.api.dex

import kotlinx.serialization.Serializable

class DexApiService(private val api: DexApi) {

    suspend fun dexChains() = api.chains()
    suspend fun dexVenues() = api.venues()
    suspend fun dexTokens(dexTokensRequest: DexTokensRequest) = api.tokens(
        chainId = dexTokensRequest.chainId,
        queryBy = dexTokensRequest.queryBy
    )
}

@Serializable
data class DexChainResponse(
    val chainId: Int,
    val name: String,
    val nativeCurrency: NativeCurrency
)

@Serializable
data class DexVenueResponse(
    val type: String,
    val name: String,
    val title: String
)

@Serializable
data class NativeCurrency(
    val chainId: Int,
    val symbol: String,
    val name: String,
    val address: String,
    val decimals: Int,
    val verifiedBy: Int
)

@Serializable
data class DexTokenResponse(
    val chainId: Int,
    val symbol: String,
    val name: String,
    val address: String,
    val decimals: Int,
    val isNative: Boolean?,
    private val verifiedBy: Int
) {
    val isVerified: Boolean
        get() = verifiedBy > 0
}

@Serializable
data class DexTokensRequest(
    val chainId: Int,
    val queryBy: String = "ALL"
)
