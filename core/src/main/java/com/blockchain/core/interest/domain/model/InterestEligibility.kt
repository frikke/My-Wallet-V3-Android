package com.blockchain.core.interest.domain.model

sealed interface InterestEligibility {
    object Eligible : InterestEligibility

    enum class Ineligible : InterestEligibility {
        REGION,
        KYC_TIER,
        OTHER,
        NONE;

        companion object {
            fun default() = OTHER
        }
    }
}
