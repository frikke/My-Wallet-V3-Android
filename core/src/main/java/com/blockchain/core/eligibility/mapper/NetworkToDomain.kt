package com.blockchain.core.eligibility.mapper

import com.blockchain.api.eligibility.data.BuyEligibilityResponse
import com.blockchain.api.eligibility.data.CountryResponse
import com.blockchain.api.eligibility.data.DefaultEligibilityResponse
import com.blockchain.api.eligibility.data.DexEligibilityResponse
import com.blockchain.api.eligibility.data.ProductEligibilityResponse
import com.blockchain.api.eligibility.data.ReasonNotEligibleReasonResponse
import com.blockchain.api.eligibility.data.ReasonNotEligibleResponse
import com.blockchain.api.eligibility.data.ReasonNotEligibleTypeResponse
import com.blockchain.api.eligibility.data.StateResponse
import com.blockchain.api.eligibility.data.SwapEligibilityResponse
import com.blockchain.api.eligibility.data.UseTradingAccountsResponse
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
        useTradingAccount?.toProductEligibility(),
        swap?.toProductEligibility(),
        dex?.toProductEligibility(),
        sell?.toProductEligibility(EligibleProduct.SELL),
        depositFiat?.toProductEligibility(EligibleProduct.DEPOSIT_FIAT),
        depositCrypto?.toProductEligibility(EligibleProduct.DEPOSIT_CRYPTO),
        depositInterest?.toProductEligibility(EligibleProduct.DEPOSIT_INTEREST),
        depositEarnCC1W?.toProductEligibility(EligibleProduct.DEPOSIT_EARN_CC1W),
        withdrawFiat?.toProductEligibility(EligibleProduct.WITHDRAW_FIAT),
        depositStaking?.toProductEligibility(EligibleProduct.DEPOSIT_STAKING),
        kycVerification?.toProductEligibility(EligibleProduct.KYC)
    )

fun UseTradingAccountsResponse.toProductEligibility(): ProductEligibility = ProductEligibility(
    product = EligibleProduct.USE_CUSTODIAL_ACCOUNTS,
    canTransact = enabled,
    isDefault = defaultProduct,
    maxTransactionsCap = TransactionsLimit.Unlimited,
    reasonNotEligible = null // not needed for the time being
)

fun BuyEligibilityResponse.toProductEligibility(): ProductEligibility = ProductEligibility(
    product = EligibleProduct.BUY,
    canTransact = enabled,
    maxTransactionsCap = if (maxOrdersCap != null && maxOrdersLeft != null) {
        TransactionsLimit.Limited(maxOrdersCap!!, maxOrdersLeft!!)
    } else {
        TransactionsLimit.Unlimited
    },
    isDefault = false,
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
    isDefault = false,
    reasonNotEligible = reasonNotEligible?.toDomain().takeIf { !enabled }
)

fun DexEligibilityResponse.toProductEligibility(): ProductEligibility = ProductEligibility(
    product = EligibleProduct.DEX,
    canTransact = enabled,
    maxTransactionsCap = TransactionsLimit.Unlimited,
    isDefault = false,
    reasonNotEligible = reasonNotEligible?.toDomain().takeIf { !enabled }
)

fun DefaultEligibilityResponse.toProductEligibility(product: EligibleProduct): ProductEligibility = ProductEligibility(
    product = product,
    canTransact = enabled,
    isDefault = false,
    maxTransactionsCap = TransactionsLimit.Unlimited,
    reasonNotEligible = reasonNotEligible?.toDomain().takeIf { !enabled }
)

fun ReasonNotEligibleResponse.toDomain(): ProductNotEligibleReason {
    val type = enumValueOfOrNull<ReasonNotEligibleTypeResponse>(type, ignoreCase = true)
    val reason = enumValueOfOrNull<ReasonNotEligibleReasonResponse>(reason, ignoreCase = true)
    return when (type) {
        ReasonNotEligibleTypeResponse.INSUFFICIENT_TIER -> when (reason) {
            ReasonNotEligibleReasonResponse.TIER_1_REQUIRED -> ProductNotEligibleReason.InsufficientTier.Tier1Required
            ReasonNotEligibleReasonResponse.TIER_2_REQUIRED -> ProductNotEligibleReason.InsufficientTier.Tier2Required
            ReasonNotEligibleReasonResponse.TIER_1_TRADE_LIMIT ->
                ProductNotEligibleReason.InsufficientTier.Tier1TradeLimitExceeded
            else -> ProductNotEligibleReason.InsufficientTier.Unknown(message)
        }
        ReasonNotEligibleTypeResponse.SANCTIONS -> when (reason) {
            ReasonNotEligibleReasonResponse.EU_5_SANCTION -> ProductNotEligibleReason.Sanctions.RussiaEU5(message)
            ReasonNotEligibleReasonResponse.EU_8_SANCTION -> ProductNotEligibleReason.Sanctions.RussiaEU8(message)
            else -> ProductNotEligibleReason.Sanctions.Unknown(message)
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
