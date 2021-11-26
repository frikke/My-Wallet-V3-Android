package com.blockchain.core.custodial.models

import info.blockchain.balance.Money

data class BrokerageQuote(
    val id: String,
    val price: Money,
    val quoteMargin: Double,
    val availability: Availability,
    val feeDetails: QuoteFee
)

data class QuoteFee(
    val fee: Money,
    val feeBeforePromo: Money,
    val promo: Promo
)

enum class Promo {
    NEW_USER, NO_PROMO
}

enum class Availability {
    INSTANT, REGULAR, UNAVAILABLE
}
