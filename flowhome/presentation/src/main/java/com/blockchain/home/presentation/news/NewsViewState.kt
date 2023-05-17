package com.blockchain.home.presentation.news

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.news.composable.NewsArticle

data class NewsViewState(
    val newsArticles: List<NewsArticle>?
) : ViewState
