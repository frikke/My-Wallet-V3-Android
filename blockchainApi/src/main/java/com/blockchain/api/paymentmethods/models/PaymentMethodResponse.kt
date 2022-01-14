package com.blockchain.api.paymentmethods.models

import kotlinx.serialization.Serializable

@Serializable
data class PaymentMethodResponse(
    val type: String,
    val eligible: Boolean,
    val visible: Boolean,
    val limits: Limits,
    val subTypes: List<String>? = null,
    val currency: String? = null
) {
    companion object {
        const val PAYMENT_CARD = "PAYMENT_CARD"
        const val FUNDS = "FUNDS"
        const val BANK_TRANSFER = "BANK_TRANSFER"
        const val BANK_ACCOUNT = "BANK_ACCOUNT"
    }
}

@Serializable
data class Limits(val min: Long, val max: Long, val daily: DailyLimits? = null)
@Serializable
data class DailyLimits(val limit: Long, val available: Long, val used: Long)

@Serializable
data class CardResponse(
    val id: String,
    val partner: String,
    val state: String,
    val currency: String,
    val card: CardDetailsResponse? = null
) {
    companion object {
        const val ACTIVE = "ACTIVE"
        const val PENDING = "PENDING"
        const val BLOCKED = "BLOCKED"
        const val CREATED = "CREATED"
        const val EXPIRED = "EXPIRED"
    }
}

@Serializable
data class CardDetailsResponse(
    val number: String,
    val expireYear: Int? = null,
    val expireMonth: Int? = null,
    val type: String? = null,
    val label: String? = null
)
