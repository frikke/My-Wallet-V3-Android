package com.blockchain.api.paymentmethods.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentMethodResponse(
    @SerialName("type")
    val type: String,
    @SerialName("eligible")
    val eligible: Boolean,
    @SerialName("visible")
    val visible: Boolean,
    @SerialName("limits")
    val limits: Limits,
    @SerialName("subTypes")
    val subTypes: List<String>? = null,
    @SerialName("currency")
    val currency: String? = null,
    @SerialName("mobilePayment")
    val mobilePayment: List<String>? = null,
    @SerialName("cardFundSources")
    val cardFundSources: List<String>? = null
) {
    companion object {
        const val PAYMENT_CARD = "PAYMENT_CARD"
        const val GOOGLE_PAY = "GOOGLE_PAY"
        const val FUNDS = "FUNDS"
        const val BANK_TRANSFER = "BANK_TRANSFER"
        const val BANK_ACCOUNT = "BANK_ACCOUNT"
    }
}

@Serializable
data class Limits(
    @SerialName("min")
    val min: Long,
    @SerialName("max")
    val max: Long,
    @SerialName("daily")
    val daily: DailyLimits? = null
)

@Serializable
data class DailyLimits(
    @SerialName("limit")
    val limit: Long,
    @SerialName("available")
    val available: Long,
    @SerialName("used")
    val used: Long
)

@Serializable
data class CardResponse(
    @SerialName("id")
    val id: String,
    @SerialName("partner")
    val partner: String,
    @SerialName("state")
    val state: String,
    @SerialName("currency")
    val currency: String,
    @SerialName("card")
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
    @SerialName("number")
    val number: String,
    @SerialName("expireYear")
    val expireYear: Int? = null,
    @SerialName("expireMonth")
    val expireMonth: Int? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("label")
    val label: String? = null
)
