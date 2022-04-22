package com.blockchain.core.eligibility.mapper

import com.blockchain.api.eligibility.data.BuyEligibilityResponse
import com.blockchain.api.eligibility.data.CustodialWalletsEligibilityResponse
import com.blockchain.api.eligibility.data.ProductEligibilityResponse
import com.blockchain.api.eligibility.data.SwapEligibilityResponse
import com.blockchain.domain.eligibility.model.EligibleProduct
import com.blockchain.domain.eligibility.model.ProductEligibility
import com.blockchain.domain.eligibility.model.TransactionsLimit

fun ProductEligibilityResponse.toDomain(): List<ProductEligibility> =
    listOfNotNull(buy?.toProductEligibility(), swap?.toProductEligibility(), custodialWallets?.toProductEligibility())

fun BuyEligibilityResponse.toProductEligibility(): ProductEligibility = ProductEligibility(
    product = EligibleProduct.BUY,
    canTransact = canPlaceOrder,
    maxTransactionsCap = if (maxOrdersCap != null && maxOrdersLeft != null) {
        TransactionsLimit.Limited(maxOrdersCap!!, maxOrdersLeft!!)
    } else {
        TransactionsLimit.Unlimited
    },
    canUpgradeTier = suggestedUpgrade != null
)

fun SwapEligibilityResponse.toProductEligibility(): ProductEligibility = ProductEligibility(
    product = EligibleProduct.SWAP,
    canTransact = canPlaceOrder,
    maxTransactionsCap = if (maxOrdersCap != null && maxOrdersLeft != null) {
        TransactionsLimit.Limited(maxOrdersCap!!, maxOrdersLeft!!)
    } else {
        TransactionsLimit.Unlimited
    },
    canUpgradeTier = suggestedUpgrade != null
)

fun CustodialWalletsEligibilityResponse.toProductEligibility(): ProductEligibility =
    ProductEligibility(
        product = EligibleProduct.CRYPTO_DEPOSIT,
        canTransact = canDepositCrypto,
        maxTransactionsCap = TransactionsLimit.Unlimited,
        canUpgradeTier = suggestedUpgrade != null
    )
