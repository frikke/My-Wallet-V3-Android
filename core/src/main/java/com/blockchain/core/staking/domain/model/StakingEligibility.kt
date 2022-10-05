package com.blockchain.core.staking.domain.model

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
