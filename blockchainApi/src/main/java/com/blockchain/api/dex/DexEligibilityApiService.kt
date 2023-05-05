package com.blockchain.api.dex

class DexEligibilityApiService(private val api: DexApi) {
    suspend fun eligibility() = api.eligibility()
}

data class DexEligibilityResponse(
    val eligible: Boolean,
    val ineligibilityReason: String
)
