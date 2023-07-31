package com.blockchain.api.news

import com.blockchain.outcome.Outcome

interface NewsApiService {
    suspend fun newsArticles(
        tickers: List<String>,
        limit: Int = 20
    ): Outcome<Exception, NewsArticlesDto>
}

internal class NewsApiServiceImpl(
    private val api: NewsApi
) : NewsApiService {
    override suspend fun newsArticles(
        tickers: List<String>,
        limit: Int
    ): Outcome<Exception, NewsArticlesDto> = api.newsArticles(
        tickers = tickers.joinToString(","),
        limit = limit
    )
}
