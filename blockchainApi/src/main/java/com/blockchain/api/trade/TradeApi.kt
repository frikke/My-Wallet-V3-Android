package com.blockchain.api.trade

import com.blockchain.api.trade.data.AccumulatedInPeriodResponse
import com.blockchain.api.trade.data.QuoteResponse
import com.blockchain.outcome.Outcome
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Query

internal interface TradeApi {

    @GET("trades/accumulated")
    fun isFirstTimeBuyer(
        @Query("products") products: String? = "SIMPLEBUY"
    ): Single<AccumulatedInPeriodResponse>

    @GET("brokerage/quote/price")
    suspend fun getQuotePrice(
        @Query("currencyPair") currencyPair: String,
        @Query("amount") amount: String,
        @Query("paymentMethod") paymentMethod: String,
        @Query("orderProfileName") orderProfileName: String = "SIMPLEBUY",
    ): Outcome<Exception, QuoteResponse>
}
