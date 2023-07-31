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
    val price: String, // price, in target curr, for each "major unit" of source curr
    val resultAmount: String, // target curr : (amount(major)-dynamicFee)*price - networkFee
    val quoteMarginPercent: Double,
    val quoteCreatedAt: String,
    val quoteExpiresAt: String,
    // networkFee refers to the 2nd leg of the transaction, to in case of NC BTC -> NC ETH,
    // it will only refer to the NC ETH networkFee, we'll still have to calculate the NC BTC networkFee ourselves
    val networkFee: String, // destination curr
    val staticFee: String, // source curr
    @SerialName("feeDetails")
    val feeDetails: FeeDetailsResponse,
    @SerialName("settlementDetails")
    val settlementDetails: SettlementDetails?,
    @SerialName("depositTerms")
    val depositTerms: DepositTermsResponse?
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
class DepositTermsResponse(
    val creditCurrency: String,
    val availableToTradeMinutesMin: Int,
    val availableToTradeMinutesMax: Int,
    val availableToTradeDisplayMode: String,
    val availableToWithdrawMinutesMin: Int,
    val availableToWithdrawMinutesMax: Int,
    val availableToWithdrawDisplayMode: String,
    val settlementType: String?,
    val settlementReason: String?
) {
    companion object {
        val DAYS = "DAYS"
        val MINUTES = "MINUTES"
        val MINUTE_RANGE = "MINUTE_RANGE"
        val DAY_RANGE = "DAY_RANGE"
        val MAX_DAY = "MAX_DAY"
        val MAX_MINUTE = "MAX_MINUTE"
        val IMMEDIATELY = "IMMEDIATELY"
        val NONE = "NONE"
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
