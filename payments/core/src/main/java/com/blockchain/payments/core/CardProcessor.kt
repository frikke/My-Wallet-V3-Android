package com.blockchain.payments.core

import com.blockchain.outcome.Outcome

enum class CardAcquirer {
    CHECKOUTDOTCOM,
    EVERYPAY,
    STRIPE,
    UNKNOWN;

    companion object {
        fun fromString(acquirerName: String): CardAcquirer {
            return when {
                acquirerName.contains(CHECKOUTDOTCOM.name, ignoreCase = true) -> CHECKOUTDOTCOM
                acquirerName.contains(EVERYPAY.name, ignoreCase = true) -> EVERYPAY
                acquirerName.contains(STRIPE.name, ignoreCase = true) -> STRIPE
                else -> UNKNOWN
            }
        }
    }
}

typealias PaymentToken = String

interface CardProcessor {

    val acquirer: CardAcquirer

    suspend fun createPaymentMethod(
        cardDetails: CardDetails,
        billingAddress: CardBillingAddress? = null,
        apiKey: String
    ): Outcome<CardProcessingFailure, PaymentToken>
}
