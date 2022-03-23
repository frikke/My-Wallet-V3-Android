package com.blockchain.nabu.models.responses.interest

import kotlinx.serialization.Serializable

@Serializable
data class InterestRateResponse(
    val rate: Double
)

@Serializable
data class InterestAddressResponse(
    val accountRef: String
)

@Serializable
data class InterestActivityResponse(
    val items: List<InterestActivityItemResponse>
)

@Serializable
data class InterestActivityItemResponse(
    val amount: InterestAmount,
    val amountMinor: String,
    val extraAttributes: InterestAttributes?,
    val id: String,
    val insertedAt: String,
    val state: String,
    val type: String
) {
    companion object {
        const val FAILED = "FAILED"
        const val REJECTED = "REJECTED"
        const val PROCESSING = "PROCESSING"
        const val COMPLETE = "COMPLETE"
        const val CREATED = "CREATED"
        const val PENDING = "PENDING"
        const val MANUAL_REVIEW = "MANUAL_REVIEW"
        const val CLEARED = "CLEARED"
        const val REFUNDED = "REFUNDED"
        const val DEPOSIT = "DEPOSIT"
        const val WITHDRAWAL = "WITHDRAWAL"
        const val INTEREST_OUTGOING = "INTEREST_OUTGOING"
    }
}

@Serializable
data class InterestAmount(
    val symbol: String,
    val value: String
)

@Serializable
data class InterestAttributes(
    val address: String?,
    val confirmations: Int?,
    val hash: String?,
    val id: String,
    val txHash: String,
    val beneficiary: InterestBeneficiary?
)

@Serializable
data class InterestBeneficiary(
    val user: String,
    val accountRef: String
)

@Serializable
data class InterestLimitsFullResponse(
    val limits: Map<String, InterestLimitsResponse>
)

@Serializable
data class InterestLimitsResponse(
    val currency: String,
    val lockUpDuration: Int,
    val maxWithdrawalAmount: String,
    val minDepositAmount: String
)

@Serializable
data class InterestEnabledResponse(
    val instruments: List<String>
)

@Serializable
data class InterestWithdrawalBody(
    val withdrawalAddress: String,
    val amount: String,
    val currency: String
)
