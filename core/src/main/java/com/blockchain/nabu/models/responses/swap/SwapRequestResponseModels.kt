package com.blockchain.nabu.models.responses.swap

import kotlinx.serialization.Serializable

@Suppress("unused")
@Serializable
class QuoteRequest(
    private val product: String,
    private val direction: String,
    private val pair: String
)

@Serializable
class QuoteResponse(
    val id: String,
    val product: String,
    val pair: String,
    val quote: Quote,
    val networkFee: String,
    val staticFee: String,
    val createdAt: String,
    val sampleDepositAddress: String,
    val expiresAt: String
)

@Serializable
data class CustodialOrderResponse(
    val id: String,
    val state: String,
    val quote: OrderQuote? = null,
    val kind: OrderKind,
    val pair: String,
    val priceFunnel: PriceFunnel,
    val createdAt: String,
    val updatedAt: String,
    val fiatValue: String,
    val fiatCurrency: String
) {
    companion object {
        const val CREATED = "CREATED"
        const val PENDING_CONFIRMATION = "PENDING_CONFIRMATION"
        const val PENDING_EXECUTION = "PENDING_EXECUTION"
        const val PENDING_DEPOSIT = "PENDING_DEPOSIT"
        const val PENDING_LEDGER = "PENDING_LEDGER"
        const val FINISH_DEPOSIT = "FINISH_DEPOSIT"
        const val PENDING_WITHDRAWAL = "PENDING_WITHDRAWAL"
        const val FAILED = "FAILED"
        const val FINISHED = "FINISHED"
        const val EXPIRED = "EXPIRED"
        const val CANCELED = "CANCELED"
    }
}

@Serializable
data class OrderQuote(
    val pair: String,
    val networkFee: String,
    val staticFee: String
)

@Serializable
data class OrderKind(
    val direction: String,
    val depositAddress: String? = null,
    val depositTxHash: String? = null,
    val withdrawalAddress: String? = null
)

@Serializable
class Quote(val priceTiers: List<InterpolationPrices>)

@Serializable
data class InterpolationPrices(
    val volume: String,
    val price: String,
    val marginPrice: String
)

@Serializable
class PriceFunnel(
    val inputMoney: String,
    val price: String,
    val networkFee: String,
    val staticFee: String,
    val outputMoney: String
)

@Serializable
class CreateOrderRequest(
    private val direction: String,
    private val quoteId: String,
    private val volume: String,
    private val destinationAddress: String? = null,
    private val refundAddress: String? = null
)

@Serializable
class UpdateSwapOrderBody(
    private val action: String
) {
    companion object {
        fun newInstance(success: Boolean): UpdateSwapOrderBody =
            if (success) {
                UpdateSwapOrderBody("DEPOSIT_SENT")
            } else {
                UpdateSwapOrderBody("CANCEL")
            }
    }
}

@Serializable
data class SwapLimitsResponse(
    val currency: String? = null,
    val minOrder: String? = null,
    val maxOrder: String? = null,
    val maxPossibleOrder: String? = null,
    val daily: TimeLimitsResponse? = null,
    val weekly: TimeLimitsResponse? = null,
    val annual: TimeLimitsResponse? = null
)

@Serializable
data class TimeLimitsResponse(
    val limit: String,
    val available: String,
    val used: String
)
