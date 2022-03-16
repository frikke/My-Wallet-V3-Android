package com.blockchain.core.eligibility.models

import java.io.Serializable

enum class EligibleProduct {
    BUY,
    SWAP,
    CRYPTO_DEPOSIT
}

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

sealed class TransactionsLimit : Serializable {
    object Unlimited : TransactionsLimit()
    data class Limited(val maxTransactionsCap: Int, val maxTransactionsLeft: Int) : TransactionsLimit()
}
