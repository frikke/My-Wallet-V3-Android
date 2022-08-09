package com.blockchain.domain.paymentmethods.model

data class GooglePayAddress(
    val address1: String,
    val address2: String,
    val address3: String,
    val administrativeArea: String,
    val countryCode: String,
    val locality: String,
    val name: String,
    val postalCode: String,
    val sortingCode: String
)
