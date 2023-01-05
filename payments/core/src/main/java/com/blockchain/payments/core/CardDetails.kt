package com.blockchain.payments.core

data class CardBillingAddress(
    val city: String,
    val countryCode: String,
    val addressLine1: String,
    val addressLine2: String,
    val postalCode: String,
    val state: String?
)

data class CardDetails(
    val number: String,
    val expMonth: Int,
    val expYear: Int,
    val cvc: String,
    val fullName: String
)
