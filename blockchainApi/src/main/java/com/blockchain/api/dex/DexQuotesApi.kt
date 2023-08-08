package com.blockchain.api.dex

import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface DexQuotesApi {
    @POST("dex/quote")
    suspend fun quote(
        @Query("product") product: String,
        @Body request: DexQuoteRequest
    ): Outcome<Exception, DexQuoteResponse>
}
