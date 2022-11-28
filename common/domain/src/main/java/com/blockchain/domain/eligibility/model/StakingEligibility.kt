package com.blockchain.domain.eligibility.model

// TODO(dserrano) - STAKING - consolidate this model with [InterestEligibility]
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
