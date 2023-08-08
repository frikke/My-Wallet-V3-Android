package com.blockchain.api.dex

import kotlinx.serialization.Serializable

class DexEligibilityApiService(private val api: DexApi) {
    suspend fun eligibility(walletAddress: String) = api.eligibility(
        product = "DEX",
        walletAddress = walletAddress
    )
}

@Serializable
data class DexEligibilityResponse(
    val eligible: Boolean,
    val ineligibilityReason: String?
)
