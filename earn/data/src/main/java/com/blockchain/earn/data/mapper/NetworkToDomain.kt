package com.blockchain.earn.data.mapper

import com.blockchain.api.earn.EarnRewardsEligibilityDto
import com.blockchain.domain.eligibility.model.EarnRewardsEligibility
import com.blockchain.earn.domain.models.EarnRewardsActivity
import com.blockchain.earn.domain.models.EarnRewardsActivityAttributes
import com.blockchain.earn.domain.models.EarnRewardsState
import com.blockchain.earn.domain.models.EarnRewardsTransactionBeneficiary
import com.blockchain.nabu.common.extensions.toTransactionType
import com.blockchain.nabu.models.responses.simplebuy.TransactionAttributesResponse
import com.blockchain.nabu.models.responses.simplebuy.TransactionResponse
import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.toLocalTime
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import java.util.Date

internal fun TransactionResponse.toEarnRewardsActivity(asset: AssetInfo, fiatValue: Money?): EarnRewardsActivity =
    EarnRewardsActivity(
        value = CryptoValue.fromMinor(asset, amountMinor.toBigInteger()),
        id = id,
        insertedAt = insertedAt.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
        state = state.toEarnRewardsState(),
        type = type.toTransactionType(),
        extraAttributes = extraAttributes?.toDomain(),
        fiatValue = fiatValue
    )

internal fun String.toEarnRewardsState(): EarnRewardsState =
    when (this) {
        TransactionResponse.FAILED -> EarnRewardsState.FAILED
        TransactionResponse.REJECTED -> EarnRewardsState.REJECTED
        TransactionResponse.PROCESSING -> EarnRewardsState.PROCESSING
        TransactionResponse.CREATED,
        TransactionResponse.COMPLETE -> EarnRewardsState.COMPLETE
        TransactionResponse.PENDING -> EarnRewardsState.PENDING
        TransactionResponse.MANUAL_REVIEW -> EarnRewardsState.MANUAL_REVIEW
        TransactionResponse.CLEARED -> EarnRewardsState.CLEARED
        TransactionResponse.REFUNDED -> EarnRewardsState.REFUNDED
        else -> EarnRewardsState.UNKNOWN
    }

internal fun TransactionAttributesResponse.toDomain() = EarnRewardsActivityAttributes(
    address = address,
    confirmations = confirmations,
    hash = hash,
    id = id,
    transactionHash = txHash,
    transferType = transferType,
    beneficiary = EarnRewardsTransactionBeneficiary(
        beneficiary?.accountRef,
        beneficiary?.user
    )
)

internal fun String.toIneligibilityReason(): EarnRewardsEligibility.Ineligible {
    return when {
        this == EarnRewardsEligibilityDto.UNSUPPORTED_REGION -> EarnRewardsEligibility.Ineligible.REGION
        this == EarnRewardsEligibilityDto.INVALID_ADDRESS -> EarnRewardsEligibility.Ineligible.REGION
        this == EarnRewardsEligibilityDto.TIER_TOO_LOW -> EarnRewardsEligibility.Ineligible.KYC_TIER
        else -> EarnRewardsEligibility.Ineligible.OTHER
    }
}
