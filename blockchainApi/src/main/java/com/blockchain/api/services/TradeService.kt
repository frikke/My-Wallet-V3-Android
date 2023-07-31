package com.blockchain.api.services

import com.blockchain.api.trade.TradeApi

class TradeService internal constructor(
    private val api: TradeApi
) {
    fun isFirstTimeBuyer() = api.isFirstTimeBuyer()

    /**
     * paymentMethod, orderProfileName:
     * Buy -> user defined(card, funds, etc..), SIMPLEBUY
     * Sell -> FUNDS, SWAP_INTERNAL
     * Sell NC -> DEPOSIT, SWAP_FROM_USERKEY
     * Swap C-C -> FUNDS, SWAP_INTERNAL
     * Swap NC-C -> DEPOSIT, SWAP_FROM_USERKEY
     * Swap NC-NC -> DEPOSIT, SWAP_ON_CHAIN
     */
    suspend fun getQuotePrice(
        currencyPair: String,
        amount: String,
        paymentMethod: String,
        orderProfileName: String
    ) = api.getQuotePrice(
        currencyPair = currencyPair,
        amount = amount,
        paymentMethod = paymentMethod,
        orderProfileName = orderProfileName
    )
}
