package com.blockchain.api.paymentmethods.models

import com.blockchain.api.NabuUxErrorResponse
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
    val cardFundSources: List<String>? = null,
    // optional since only ACH will support it initially, if null then we assume all capabilities are present
    val capabilities: List<String>? = null
) {
    companion object {
        const val PAYMENT_CARD = "PAYMENT_CARD"
        const val GOOGLE_PAY = "GOOGLE_PAY"
        const val FUNDS = "FUNDS"
        const val BANK_TRANSFER = "BANK_TRANSFER"
        const val BANK_ACCOUNT = "BANK_ACCOUNT"

        const val CAPABILITY_DEPOSIT = "DEPOSIT"
        const val CAPABILITY_WITHDRAWAL = "WITHDRAWAL"
        const val CAPABILITY_BROKERAGE = "BROKERAGE"
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
    val card: CardDetailsResponse? = null,
    @SerialName("mobilePaymentType")
    val mobilePaymentType: String? = null,
    @SerialName("block")
    val block: Boolean = false,
    @SerialName("ux")
    val ux: NabuUxErrorResponse? = null
) {
    companion object {
        const val ACTIVE = "ACTIVE"
        const val PENDING = "PENDING"
        const val BLOCKED = "BLOCKED"
        const val CREATED = "CREATED"
        const val EXPIRED = "EXPIRED"

        const val GOOGLE_PAY = "GOOGLE_PAY"
        const val APPLE_PAY = "APPLE_PAY"
        const val UNKNOWN = "UNKNOWN"
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
