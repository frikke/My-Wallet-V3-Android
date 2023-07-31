package com.blockchain.news

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import kotlinx.coroutines.flow.Flow

interface NewsService {
    fun articles(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale),
        tickers: List<String>
    ): Flow<DataResource<List<NewsArticle>>>

    suspend fun preferredNewsAssetTickers(): List<String>
}
