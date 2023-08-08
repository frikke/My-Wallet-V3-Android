package com.blockchain.news.koin

import com.blockchain.news.NewsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val newsPresentationModule = module {
    viewModel {
        NewsViewModel(
            newsService = get()
        )
    }
}
