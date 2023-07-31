package com.blockchain.api.brokerage

import com.blockchain.api.brokerage.data.BrokerageQuoteResponse
import com.blockchain.api.brokerage.data.QuoteRequestBody
import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.POST

internal interface BrokerageApi {

    @POST("brokerage/quote")
    suspend fun fetchQuote(
        @Body quoteRequestBody: QuoteRequestBody
    ): Outcome<Exception, BrokerageQuoteResponse>
}
