package com.blockchain.api.eligibility.data

import kotlinx.serialization.Serializable

@Serializable
class ProductEligibilityResponse(
    val buy: BuyEligibilityResponse?,
    val swap: SwapEligibilityResponse?,
    val custodialWallets: CustodialWalletsEligibilityResponse?
)

@Serializable
class BuyEligibilityResponse(
    val id: String,
    val canPlaceOrder: Boolean,
    val maxOrdersCap: Int?, // if null there's no max
    val maxOrdersLeft: Int?, // if null there's infinite orders left
    val suggestedUpgrade: SuggestedUpgrade?
)

@Serializable
class SwapEligibilityResponse(
    val id: String,
    val canPlaceOrder: Boolean,
    val maxOrdersCap: Int?, // if null there's no max
    val maxOrdersLeft: Int?, // if null there's infinite orders left
    val suggestedUpgrade: SuggestedUpgrade?
)

@Serializable
class CustodialWalletsEligibilityResponse(
    val id: String,
    val canDepositFiat: Boolean,
    val canDepositCrypto: Boolean,
    val canWithdrawFiat: Boolean,
    val canWithdrawCrypto: Boolean,
    val suggestedUpgrade: SuggestedUpgrade?
)

@Serializable
class SuggestedUpgrade(
    val requiredTier: Int
)
