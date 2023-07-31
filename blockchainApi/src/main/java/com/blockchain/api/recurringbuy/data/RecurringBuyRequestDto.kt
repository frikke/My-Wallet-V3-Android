package com.blockchain.api.recurringbuy.data

import kotlinx.serialization.Serializable

@Serializable
data class RecurringBuyRequestDto(
    val orderId: String,
    val inputValue: String,
    val inputCurrency: String,
    val destinationCurrency: String,
    val paymentMethod: String,
    val period: String,
    val nextPayment: String? = null,
    val paymentMethodId: String? = null
)
