package com.blockchain.api.brokerage.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class QuoteRequestBody(
    val profile: String,
    val inputValue: String,
    val pair: String,
    val paymentMethod: String,
    val paymentMethodId: String?
)

@Serializable
class BrokerageQuoteResponse(
    val quoteId: String,
    val price: String,
    val quoteMarginPercent: Double,
    val quoteCreatedAt: String,
    val quoteExpiresAt: String,
    @SerialName("feeDetails")
    val feeDetails: FeeDetailsResponse,
    @SerialName("settlementDetails")
    val settlementDetails: SettlementDetails?
)

@Serializable
class SettlementDetails(
    val availability: String?,
    val reason: String?
) {
    companion object {
        val INSTANT = "INSTANT"
        val REGULAR = "REGULAR"
        val UNAVAILABLE = "UNAVAILABLE"
    }
}

@Serializable
class FeeDetailsResponse(
    val feeWithoutPromo: String,
    val fee: String,
    val feeFlags: List<String>
) {
    companion object {
        val NEW_USER_WAIVER = "NEW_USER_WAIVER"
    }
}
