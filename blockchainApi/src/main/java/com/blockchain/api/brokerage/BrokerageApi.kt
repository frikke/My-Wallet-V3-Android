package com.blockchain.api.brokerage

import com.blockchain.api.brokerage.data.BrokerageQuoteResponse
import com.blockchain.api.brokerage.data.QuoteRequestBody
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

internal interface BrokerageApi {

    @POST("brokerage/quote")
    fun fetchQuote(
        @Header("authorization") authorization: String,
        @Body quoteRequestBody: QuoteRequestBody
    ): Single<BrokerageQuoteResponse>
}
