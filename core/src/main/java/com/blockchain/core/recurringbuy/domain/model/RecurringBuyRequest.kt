package com.blockchain.core.recurringbuy.domain.model

data class RecurringBuyRequest(
    val orderId: String,
    val inputValue: String,
    val inputCurrency: String,
    val destinationCurrency: String,
    val paymentMethod: String,
    val period: String,
    val nextPayment: String? = null,
    val paymentMethodId: String? = null
)
