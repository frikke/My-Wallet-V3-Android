package com.blockchain.api.services

import com.blockchain.api.trade.TradeApi
import com.blockchain.api.wrapErrorMessage

class TradeService internal constructor(
    private val api: TradeApi
) {
    fun isFirstTimeBuyer() = api.isFirstTimeBuyer()

    fun getNextPaymentDate() =
        api.getNextPaymentDate()

    fun getRecurringBuysForAsset(
        assetTicker: String
    ) = api.getRecurringBuysForAsset(
        assetTicker = assetTicker
    ).wrapErrorMessage()

    fun getRecurringBuyForId(
        recurringBuyId: String
    ) = api.getRecurringBuyById(
        recurringBuyId = recurringBuyId
    ).wrapErrorMessage()

    fun cancelRecurringBuy(
        id: String
    ) = api.cancelRecurringBuy(
        id = id
    ).wrapErrorMessage()

    /**
     * paymentMethod, orderProfileName:
     * Buy -> user defined(card, funds, etc..), SIMPLEBUY
     * Sell -> FUNDS, SWAP_INTERNAL
     * Sell NC -> DEPOSIT, SWAP_FROM_USERKEY
     * Swap C-C -> FUNDS, SWAP_INTERNAL
     * Swap NC-C -> DEPOSIT, SWAP_FROM_USERKEY
     * Swap NC-NC -> DEPOSIT, SWAP_ON_CHAIN
     */
    fun getQuotePrice(
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
