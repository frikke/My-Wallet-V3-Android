package com.blockchain.api.news

import com.blockchain.outcome.Outcome
import retrofit2.http.GET
import retrofit2.http.Query

internal interface NewsApi {
    /**
     * @param tickers comma-seperated TICKER
     */
    @GET("news/articles")
    suspend fun newsArticles(
        @Query("assets") tickers: String,
        @Query("limit") limit: Int
    ): Outcome<Exception, NewsArticlesDto>
}
