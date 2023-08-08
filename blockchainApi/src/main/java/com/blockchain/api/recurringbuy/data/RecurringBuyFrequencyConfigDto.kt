package com.blockchain.api.recurringbuy.data

import kotlinx.serialization.Serializable

@Serializable
data class RecurringBuyFrequencyConfigListDto(
    val nextPayments: List<RecurringBuyFrequencyConfigDto>
)

@Serializable
data class RecurringBuyFrequencyConfigDto(
    val period: String,
    val nextPayment: String,
    val eligibleMethods: List<String>
) {
    companion object {
        const val PAYMENT_CARD = "PAYMENT_CARD"
        const val FUNDS = "FUNDS"
        const val BANK_TRANSFER = "BANK_TRANSFER"
    }
}
