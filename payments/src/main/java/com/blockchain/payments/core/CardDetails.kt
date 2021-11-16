package com.blockchain.payments.core

data class CardDetails(
    val number: String,
    val expMonth: Int,
    val expYear: Int,
    val cvc: String,
    val fullName: String
)