package com.blockchain.core.eligibility.mapper

import com.blockchain.api.eligibility.data.BuyEligibilityResponse
import com.blockchain.api.eligibility.data.CountryResponse
import com.blockchain.api.eligibility.data.DefaultEligibilityResponse
import com.blockchain.api.eligibility.data.ProductEligibilityResponse
import com.blockchain.api.eligibility.data.ReasonNotEligibleReasonResponse
import com.blockchain.api.eligibility.data.ReasonNotEligibleResponse
import com.blockchain.api.eligibility.data.ReasonNotEligibleTypeResponse
import com.blockchain.api.eligibility.data.StateResponse
import com.blockchain.api.eligibility.data.SwapEligibilityResponse
import com.blockchain.domain.eligibility.model.EligibleProduct
import com.blockchain.domain.eligibility.model.GetRegionScope
import com.blockchain.domain.eligibility.model.ProductEligibility
import com.blockchain.domain.eligibility.model.ProductNotEligibleReason
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.domain.eligibility.model.TransactionsLimit
import com.blockchain.extensions.enumValueOfOrNull

fun ProductEligibilityResponse.toDomain(): List<ProductEligibility> =
    listOfNotNull(
        buy?.toProductEligibility(),
        swap?.toProductEligibility(),
        sell?.toProductEligibility(EligibleProduct.SELL),
        depositFiat?.toProductEligibility(EligibleProduct.DEPOSIT_FIAT),
        depositCrypto?.toProductEligibility(EligibleProduct.DEPOSIT_CRYPTO),
        depositInterest?.toProductEligibility(EligibleProduct.DEPOSIT_INTEREST),
        withdrawFiat?.toProductEligibility(EligibleProduct.WITHDRAW_FIAT)
    )

fun BuyEligibilityResponse.toProductEligibility(): ProductEligibility = ProductEligibility(
    product = EligibleProduct.BUY,
    canTransact = enabled,
    maxTransactionsCap = if (maxOrdersCap != null && maxOrdersLeft != null) {
        TransactionsLimit.Limited(maxOrdersCap!!, maxOrdersLeft!!)
    } else {
        TransactionsLimit.Unlimited
    },
    reasonNotEligible = reasonNotEligible?.toDomain().takeIf { !enabled }
)

fun SwapEligibilityResponse.toProductEligibility(): ProductEligibility = ProductEligibility(
    product = EligibleProduct.SWAP,
    canTransact = enabled,
    maxTransactionsCap = if (maxOrdersCap != null && maxOrdersLeft != null) {
        TransactionsLimit.Limited(maxOrdersCap!!, maxOrdersLeft!!)
    } else {
        TransactionsLimit.Unlimited
    },
    reasonNotEligible = reasonNotEligible?.toDomain().takeIf { !enabled }
)

fun DefaultEligibilityResponse.toProductEligibility(product: EligibleProduct): ProductEligibility = ProductEligibility(
    product = product,
    canTransact = enabled,
    maxTransactionsCap = TransactionsLimit.Unlimited,
    reasonNotEligible = reasonNotEligible?.toDomain().takeIf { !enabled }
)

fun ReasonNotEligibleResponse.toDomain(): ProductNotEligibleReason {
    val type = enumValueOfOrNull<ReasonNotEligibleTypeResponse>(type, ignoreCase = true)
    val reason = enumValueOfOrNull<ReasonNotEligibleReasonResponse>(reason, ignoreCase = true)
    return when (type) {
        ReasonNotEligibleTypeResponse.INSUFFICIENT_TIER -> when (reason) {
            ReasonNotEligibleReasonResponse.TIER_2_REQUIRED -> ProductNotEligibleReason.InsufficientTier.Tier2Required
            ReasonNotEligibleReasonResponse.TIER_1_TRADE_LIMIT ->
                ProductNotEligibleReason.InsufficientTier.Tier1TradeLimitExceeded
            else -> ProductNotEligibleReason.Unknown(message)
        }
        ReasonNotEligibleTypeResponse.SANCTIONS -> when (reason) {
            ReasonNotEligibleReasonResponse.EU_5_SANCTION -> ProductNotEligibleReason.Sanctions.RussiaEU5
            else -> ProductNotEligibleReason.Unknown(message)
        }
        null -> ProductNotEligibleReason.Unknown(message)
    }
}

fun CountryResponse.toDomain(): Region.Country = Region.Country(
    countryCode = code,
    name = name,
    isKycAllowed = scopes.any { it.toGetRegionScope() == GetRegionScope.Kyc },
    states = states
)

fun StateResponse.toDomain(): Region.State = Region.State(
    countryCode = countryCode,
    name = name,
    isKycAllowed = scopes.any { it.toGetRegionScope() == GetRegionScope.Kyc },
    stateCode = code
)

fun String.toGetRegionScope(): GetRegionScope? = GetRegionScope.values().find {
    val key = it.toNetwork()
    this.equals(key, ignoreCase = true)
}
