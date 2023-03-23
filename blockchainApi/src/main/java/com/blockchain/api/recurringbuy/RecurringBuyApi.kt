package com.blockchain.api.recurringbuy

import com.blockchain.api.recurringbuy.data.RecurringBuyDto
import com.blockchain.api.recurringbuy.data.RecurringBuyFrequencyConfigListDto
import com.blockchain.api.recurringbuy.data.RecurringBuyRequestDto
import com.blockchain.outcome.Outcome
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

internal interface RecurringBuyApi {
    @GET("recurring-buy/next-payment")
    fun frequencyConfig(): Single<RecurringBuyFrequencyConfigListDto>

    @GET("recurring-buy/list")
    fun getRecurringBuys(): Single<List<RecurringBuyDto>>

    @POST("recurring-buy/create")
    suspend fun createRecurringBuy(
        @Body request: RecurringBuyRequestDto
    ): Outcome<Exception, RecurringBuyDto>

    @DELETE("recurring-buy/{id}/cancel")
    suspend fun cancelRecurringBuy(
        @Path("id") id: String
    ): Outcome<Exception, Unit>
}
