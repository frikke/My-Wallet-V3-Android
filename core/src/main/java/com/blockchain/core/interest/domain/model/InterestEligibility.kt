package com.blockchain.core.interest.domain.model

// data class AssetInterestEligibility(
//    val cryptoCurrency: AssetInfo,
//    val eligibility: Eligibility
// )

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

// data class Eligibility(
//    val eligible: Boolean,
//    val ineligibilityReason: IneligibilityReason
// ) {
//    companion object {
//        fun notEligibleInstance() = Eligibility(false, IneligibilityReason.OTHER)
//    }
// }
//
// enum class IneligibilityReason {
//    REGION,
//    KYC_TIER,
//    OTHER,
//    NONE
// }
