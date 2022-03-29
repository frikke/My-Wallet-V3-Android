package com.blockchain.nabu.datamanagers.repositories.interest

import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single

enum class IneligibilityReason {
    REGION,
    KYC_TIER,
    OTHER,
    NONE
}

interface InterestEligibilityProvider {
    fun getEligibilityForCustodialAssets(): Single<List<AssetInterestEligibility>>
}

class InterestEligibilityProviderImpl(
    private val custodialAssetsEligibilityCache: CustodialAssetsEligibilityCache
) : InterestEligibilityProvider {
    override fun getEligibilityForCustodialAssets(): Single<List<AssetInterestEligibility>> =
        custodialAssetsEligibilityCache.cached()
}

data class AssetInterestEligibility(
    val cryptoCurrency: AssetInfo,
    val eligibility: Eligibility
)

data class Eligibility(
    val eligible: Boolean,
    val ineligibilityReason: IneligibilityReason
) {
    companion object {
        fun notEligible() = Eligibility(false, IneligibilityReason.OTHER)
    }
}
