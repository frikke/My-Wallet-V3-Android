package com.blockchain.core.custodial.models

import com.blockchain.domain.common.model.Millis
import com.blockchain.domain.paymentmethods.model.DepositTerms
import com.blockchain.domain.paymentmethods.model.SettlementReason
import com.blockchain.nabu.datamanagers.BuySellOrder
import com.blockchain.utils.CurrentTimeProvider
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.Money

data class BrokerageQuote(
    val id: String,
    val currencyPair: CurrencyPair,
    val inputAmount: Money,
    val price: Money,
    val rawPrice: Money,
    val resultAmount: Money,
    val quoteMargin: Double?,
    val availability: Availability?,
    val settlementReason: SettlementReason?,
    val networkFee: Money,
    val staticFee: Money,
    val feeDetails: QuoteFee,
    val createdAt: Millis,
    val expiresAt: Millis,
    val depositTerms: DepositTerms?
) {
    fun millisToExpire(): Millis {
        return expiresAt - CurrentTimeProvider.currentTimeMillis()
    }

    val secondsToExpire: Float
        get() = millisToExpire().div(1000f)
}

data class BuyOrderAndQuote(
    val buyOrder: BuySellOrder,
    val quote: BrokerageQuote,
)

data class QuoteFee(
    val fee: Money,
    val feeBeforePromo: Money,
    val promo: Promo,
)

enum class Promo {
    NEW_USER, NO_PROMO
}

enum class Availability {
    INSTANT, REGULAR, UNAVAILABLE
}
