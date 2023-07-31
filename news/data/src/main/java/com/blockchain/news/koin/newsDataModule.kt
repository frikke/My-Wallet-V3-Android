package com.blockchain.news.koin

import com.blockchain.news.NewsRepository
import com.blockchain.news.NewsService
import com.blockchain.news.dataresources.NewsStore
import org.koin.dsl.module

val newsDataModule = module {
    single {
        NewsStore(
            newsApiService = get()
        )
    }

    single<NewsService> {
        NewsRepository(
            newsStore = get(),
            remoteConfigService = get()
        )
    }
}
