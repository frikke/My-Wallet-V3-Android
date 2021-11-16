package com.blockchain.payments.core

import com.blockchain.outcome.Outcome

enum class Partner {
    EVERYPAY,
    CHECKOUT_COM,
    STRIPE,
    UNKNOWN
}

typealias PaymentToken = String

interface CardProcessor {

    val partner: Partner

    suspend fun createPaymentMethod(
        cardDetails: CardDetails,
        apiKey: String
    ): Outcome<CardProcessingFailure, PaymentToken>
}