package com.blockchain.home.presentation.news

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.home.presentation.quickactions.QuickActionItem
import com.blockchain.news.NewsArticle

data class NewsViewState(
    val newsArticles: List<NewsArticle>?
) : ViewState
