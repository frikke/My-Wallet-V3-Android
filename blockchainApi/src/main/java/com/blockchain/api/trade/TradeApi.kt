package com.blockchain.api.trade

import com.blockchain.api.trade.data.AccumulatedInPeriodResponse
import com.blockchain.api.trade.data.NextPaymentRecurringBuyResponse
import com.blockchain.api.trade.data.RecurringBuyResponse
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

internal interface TradeApi {

    @GET("trades/accumulated")
    fun isFirstTimeBuyer(
        @Header("authorization") authorization: String,
        @Query("products") products: String? = "SIMPLEBUY"
    ): Single<AccumulatedInPeriodResponse>

    @GET("recurring-buy/next-payment")
    fun getNextPaymentDate(
        @Header("authorization") authorization: String
    ): Single<NextPaymentRecurringBuyResponse>

    @GET("recurring-buy/list")
    fun getRecurringBuysForAsset(
        @Header("authorization") authorization: String,
        @Query("currency") assetTicker: String? = null
    ): Single<List<RecurringBuyResponse>>

    @GET("recurring-buy/list")
    fun getRecurringBuyById(
        @Header("authorization") authorization: String,
        @Query("id") recurringBuyId: String
    ): Single<List<RecurringBuyResponse>>

    @DELETE("recurring-buy/{id}/cancel")
    fun cancelRecurringBuy(
        @Header("authorization") authorization: String,
        @Path("id") id: String
    ): Completable
}
