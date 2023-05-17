package com.blockchain.news

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.news.composable.NewsArticle

data class NewsViewState(
    val newsArticles: DataResource<List<NewsArticle>>
) : ViewState
