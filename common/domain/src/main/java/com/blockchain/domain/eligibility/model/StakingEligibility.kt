package com.blockchain.domain.eligibility.model

sealed interface StakingEligibility {
    object Eligible : StakingEligibility

    enum class Ineligible : StakingEligibility {
        REGION,
        KYC_TIER,
        OTHER,
        NONE;

        companion object {
            fun default() = OTHER
        }
    }
}
