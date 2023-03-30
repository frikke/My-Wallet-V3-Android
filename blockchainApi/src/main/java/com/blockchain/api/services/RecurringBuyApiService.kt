package com.blockchain.api.services

import com.blockchain.api.recurringbuy.RecurringBuyApi
import com.blockchain.api.recurringbuy.data.RecurringBuyRequestDto
import com.blockchain.api.wrapErrorMessage

class RecurringBuyApiService internal constructor(
    private val api: RecurringBuyApi
) {
    fun frequencyConfig() =
        api.frequencyConfig()

    fun getRecurringBuys() =
        api.getRecurringBuys().wrapErrorMessage()

    suspend fun createRecurringBuy(
        request: RecurringBuyRequestDto
    ) = api.createRecurringBuy(request = request)

    suspend fun cancel(
        id: String
    ) = api.cancelRecurringBuy(id = id)
}
