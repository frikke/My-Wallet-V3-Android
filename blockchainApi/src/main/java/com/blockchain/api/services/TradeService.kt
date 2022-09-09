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
}
