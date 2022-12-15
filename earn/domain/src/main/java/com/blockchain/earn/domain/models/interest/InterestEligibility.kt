package com.blockchain.earn.domain.models.interest

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
