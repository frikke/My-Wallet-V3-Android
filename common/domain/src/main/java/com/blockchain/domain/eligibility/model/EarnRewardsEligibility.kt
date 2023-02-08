package com.blockchain.domain.eligibility.model

sealed interface EarnRewardsEligibility {
    object Eligible : EarnRewardsEligibility

    enum class Ineligible : EarnRewardsEligibility {
        REGION,
        KYC_TIER,
        OTHER,
        ;

        companion object {
            fun default() = OTHER
        }
    }
}
