package com.blockchain.home.presentation.news

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.news.NewsArticle
import com.blockchain.walletmode.WalletMode

data class NewsModelState(
    val newsArticles: DataResource<List<NewsArticle>> = DataResource.Loading,
    val walletMode: WalletMode? = null,
    val availableModes: List<WalletMode> = emptyList(),
    val lastFreshDataTime: Long = 0
) : ModelState
