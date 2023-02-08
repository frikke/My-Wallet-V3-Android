package com.blockchain.api.earn

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EarnRewardsEligibilityDto(
    @SerialName("eligible")
    val isEligible: Boolean = false,
    @SerialName("ineligibilityReason")
    val reason: String = DEFAULT_REASON_NONE
) {
    companion object {
        const val UNSUPPORTED_REGION = "UNSUPPORTED_REGION"
        const val INVALID_ADDRESS = "INVALID_ADDRESS"
        const val TIER_TOO_LOW = "TIER_TOO_LOW"
        const val DEFAULT_REASON_NONE = "NONE"
        private const val DEFAULT_FAILURE_REASON = "OTHER"

        fun default() = EarnRewardsEligibilityDto(
            isEligible = false,
            reason = DEFAULT_FAILURE_REASON
        )
    }
}
