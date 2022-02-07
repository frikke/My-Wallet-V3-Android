package com.blockchain.nabu.models.responses.cards

data class PaymentMethodsResponse(
    val currency: String,
    val methods: List<PaymentMethodResponse>
)

data class BeneficiariesResponse(
    val id: String,
    val address: String,
    val currency: String,
    val name: String,
    val agent: AgentResponse
)

data class AgentResponse(val account: String)

data class PaymentMethodResponse(
    val type: String,
    val eligible: Boolean,
    val visible: Boolean,
    val limits: Limits,
    val subTypes: List<String>,
    val currency: String
) {
    companion object {
        const val PAYMENT_CARD = "PAYMENT_CARD"
        const val FUNDS = "FUNDS"
        const val BANK_TRANSFER = "BANK_TRANSFER"
        const val BANK_ACCOUNT = "BANK_ACCOUNT"
    }
}

data class Limits(val min: Long, val max: Long, val daily: DailyLimits)
data class DailyLimits(val limit: Long, val available: Long, val used: Long)

data class PaymentCardAcquirerResponse(
    val cardAcquirerName: String,
    val cardAcquirerAccountCodes: List<String>,
    val apiKey: String
)
