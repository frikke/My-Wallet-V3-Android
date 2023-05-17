package com.blockchain.news

import com.blockchain.api.news.NewsArticlesDto
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.mapData
import com.blockchain.domain.experiments.RemoteConfigService
import com.blockchain.news.dataresources.NewsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.rx3.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class NewsRepository(
    private val newsStore: NewsStore,
    private val remoteConfigService: RemoteConfigService
) : NewsService {
    override fun articles(
        freshnessStrategy: FreshnessStrategy,
        tickers: List<String>
    ): Flow<DataResource<List<NewsArticle>>> {
        return newsStore.stream(freshnessStrategy.withKey(tickers))
            .mapData { it.toNewsArticles() }
    }

    override suspend fun preferredNewsAssetTickers(): List<String> {
        remoteConfigService.getRawJson(PREFERRED_NEWS_ASSET_TICKERS)
            .onErrorReturn { DEFAULT_TICKERS }
            .await()
            .run {
                return Json.decodeFromString(this)
            }
    }

    companion object {
        private const val DEFAULT_TICKERS =
            """[
                  "AAVE","ADA","ALGO","APE","BCH","BTC","CEUR","CHZ","CLOUT","COMP","CRV",
                  "DAI","DOGE","DOT","ETC","ETH","GALA","LINK","LTC","NEAR","PAX","SNX","SOL",
                  "STX","SUSHI","TRX","UNI","USDC","USDT","WBTC","XLM","YFI"
            ]"""
        private const val PREFERRED_NEWS_ASSET_TICKERS = "blockchain_app_configuration_dashboard_news_asset_filter"
    }
}

private fun NewsArticlesDto.toNewsArticles(): List<NewsArticle> {
    return articles.map {
        NewsArticle(
            id = it.id,
            title = it.title,
            image = it.image,
            date = it.date,
            author = it.author,
            link = it.link
        )
    }
}
