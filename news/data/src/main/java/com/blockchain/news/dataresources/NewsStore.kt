package com.blockchain.news.dataresources

import com.blockchain.api.news.NewsApiService
import com.blockchain.api.news.NewsArticlesDto
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

class NewsStore(
    private val newsApiService: NewsApiService
) : KeyedStore<List<String>, NewsArticlesDto> by PersistedJsonSqlDelightStoreBuilder().buildKeyed(
    storeId = "NewsStore",
    fetcher = Fetcher.Keyed.ofOutcome { tickers ->
        newsApiService.newsArticles(
            tickers = tickers
        )
    },
    keySerializer = ListSerializer(String.serializer()),
    dataSerializer = NewsArticlesDto.serializer(),
    mediator = FreshnessMediator(Freshness.DURATION_1_HOUR)
)
