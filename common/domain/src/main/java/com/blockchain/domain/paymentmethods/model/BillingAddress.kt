package com.blockchain.domain.paymentmethods.model

import kotlinx.serialization.Serializable

@Serializable
data class BillingAddress(
    val countryCode: String,
    val fullName: String,
    val addressLine1: String,
    val addressLine2: String,
    val city: String,
    val postCode: String,
    val state: String?
)
