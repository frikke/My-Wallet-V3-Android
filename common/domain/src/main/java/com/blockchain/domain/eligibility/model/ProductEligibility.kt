package com.blockchain.domain.eligibility.model

import kotlinx.serialization.Serializable

@Serializable
data class ProductEligibility(
    val product: EligibleProduct,
    val canTransact: Boolean,
    val isDefault: Boolean,
    val maxTransactionsCap: TransactionsLimit,
    val reasonNotEligible: ProductNotEligibleReason?
) {
    companion object {
        fun asEligible(product: EligibleProduct): ProductEligibility =
            ProductEligibility(
                product = product,
                canTransact = true,
                isDefault = false,
                maxTransactionsCap = TransactionsLimit.Unlimited,
                reasonNotEligible = null
            )

        fun asNotEligible(product: EligibleProduct): ProductEligibility =
            ProductEligibility(
                product = product,
                canTransact = false,
                isDefault = false,
                maxTransactionsCap = TransactionsLimit.Unlimited,
                reasonNotEligible = ProductNotEligibleReason.Unknown("Unknown Reason")
            )
    }
}

@Serializable
sealed class ProductNotEligibleReason {
    @Serializable
    sealed class InsufficientTier : ProductNotEligibleReason() {
        @Serializable
        object Tier1Required : InsufficientTier()
        @Serializable
        object Tier2Required : InsufficientTier()
        @Serializable
        object Tier1TradeLimitExceeded : InsufficientTier()
        @Serializable
        data class Unknown(val message: String) : InsufficientTier()
    }
    @Serializable
    sealed class Sanctions : ProductNotEligibleReason() {
        abstract val message: String

        @Serializable
        data class RussiaEU5(override val message: String) : Sanctions()
        @Serializable
        data class RussiaEU8(override val message: String) : Sanctions()
        @Serializable
        data class Unknown(override val message: String) : Sanctions()
    }
    @Serializable
    data class Unknown(val message: String) : ProductNotEligibleReason()
}
