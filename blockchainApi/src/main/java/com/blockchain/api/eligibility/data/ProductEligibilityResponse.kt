package com.blockchain.api.eligibility.data

import kotlinx.serialization.Serializable

@Serializable
class ProductEligibilityResponse(
    // if the user has many products not eligible by one particular reason, eg. sanctions, there will be a reason
    // in this `notifications` field so we can display them in a more general(non-product specific) location, like an Announcement
    val notifications: List<ReasonNotEligibleResponse>,
    val buy: BuyEligibilityResponse?,
    val swap: SwapEligibilityResponse?,
    val sell: DefaultEligibilityResponse?,
    val useTradingAccount: UseTradingAccountsResponse?,
    val depositFiat: DefaultEligibilityResponse?,
    val depositCrypto: DefaultEligibilityResponse?,
    val depositInterest: DefaultEligibilityResponse?,
    val withdrawFiat: DefaultEligibilityResponse?,
    val depositStaking: DefaultEligibilityResponse?,
    val kycVerification: DefaultEligibilityResponse?
)

@Serializable
class ReasonNotEligibleResponse(
    val reason: String, // specific reason, eg. EU_5_SANCTION
    val type: String, // general category of the reason, eg. SANCTIONS, INSUFFICIENT_TIER
    val message: String // human readable message to be used as a fallback
)

enum class ReasonNotEligibleTypeResponse {
    INSUFFICIENT_TIER,
    SANCTIONS
}

enum class ReasonNotEligibleReasonResponse {
    // INSUFFICIENT_TIER:
    TIER_1_REQUIRED,
    TIER_2_REQUIRED,
    TIER_1_TRADE_LIMIT,

    // SANCTIONS:
    EU_5_SANCTION,
    EU_8_SANCTION
}

@Serializable
class BuyEligibilityResponse(
    val enabled: Boolean,
    val maxOrdersCap: Int?, // if null there's no max
    val maxOrdersLeft: Int?, // if null there's infinite orders left
    val reasonNotEligible: ReasonNotEligibleResponse?
)

@Serializable
class SwapEligibilityResponse(
    val enabled: Boolean,
    val maxOrdersCap: Int?, // if null there's no max
    val maxOrdersLeft: Int?, // if null there's infinite orders left
    val reasonNotEligible: ReasonNotEligibleResponse?
)

@Serializable
class DefaultEligibilityResponse(
    val enabled: Boolean,
    val reasonNotEligible: ReasonNotEligibleResponse?
)

@Serializable
class UseTradingAccountsResponse(
    val enabled: Boolean,
    val defaultProduct: Boolean
)
