package com.blockchain.api.trade.data

import kotlinx.serialization.Serializable

@Serializable
data class RecurringBuyResponse(
    val id: String,
    val userId: String,
    val inputCurrency: String,
    val inputValue: String,
    val destinationCurrency: String,
    val paymentMethod: String,
    val paymentMethodId: String?,
    val period: String,
    val nextPayment: String,
    val state: String,
    val insertedAt: String,
    val updatedAt: String
) {
    companion object {
        const val ACTIVE = "ACTIVE"
        const val INACTIVE = "INACTIVE"
        const val DAILY = "DAILY"
        const val WEEKLY = "WEEKLY"
        const val BI_WEEKLY = "BI_WEEKLY"
        const val MONTHLY = "MONTHLY"
    }
}
