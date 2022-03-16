package com.blockchain.api.services

import com.blockchain.api.trade.TradeApi
import com.blockchain.api.wrapErrorMessage

class TradeService internal constructor(
    private val api: TradeApi
) {
    fun isFirstTimeBuyer(authHeader: String) =
        api.isFirstTimeBuyer(authHeader)

    fun getNextPaymentDate(authHeader: String) =
        api.getNextPaymentDate(authHeader)

    fun getRecurringBuysForAsset(
        authHeader: String,
        assetTicker: String
    ) = api.getRecurringBuysForAsset(
        authorization = authHeader,
        assetTicker = assetTicker
    ).wrapErrorMessage()

    fun getRecurringBuyForId(
        authHeader: String,
        recurringBuyId: String
    ) = api.getRecurringBuyById(
        authorization = authHeader,
        recurringBuyId = recurringBuyId
    ).wrapErrorMessage()

    fun cancelRecurringBuy(
        authHeader: String,
        id: String
    ) = api.cancelRecurringBuy(
        authorization = authHeader,
        id = id
    ).wrapErrorMessage()
}
