package com.blockchain.domain.eligibility.model

data class ProductEligibility(
    val product: EligibleProduct,
    val canTransact: Boolean,
    val maxTransactionsCap: TransactionsLimit,
    val canUpgradeTier: Boolean
) {
    companion object {
        fun asEligible(product: EligibleProduct): ProductEligibility =
            ProductEligibility(
                product = product,
                canTransact = true,
                maxTransactionsCap = TransactionsLimit.Unlimited,
                canUpgradeTier = false
            )
    }
}
