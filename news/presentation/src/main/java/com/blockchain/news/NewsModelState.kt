package com.blockchain.news

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.news.NewsArticle
import com.blockchain.news.composable.NewsArticle

data class NewsModelState(
    val newsArticles: DataResource<List<NewsArticle>> = DataResource.Loading,
    val lastFreshDataTime: Long = 0
) : ModelState