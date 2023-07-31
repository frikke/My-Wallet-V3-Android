package com.blockchain.home.presentation.news

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource
import com.blockchain.data.dataOrElse
import com.blockchain.presentation.pulltorefresh.PullToRefresh
import com.blockchain.walletmode.WalletMode

sealed interface NewsIntent : Intent<NewsModelState> {
    data class LoadData(
        val walletMode: WalletMode
    ) : NewsIntent {
        override fun isValidFor(modelState: NewsModelState): Boolean {
            return (modelState.newsArticles as? DataResource.Data)?.dataOrElse(emptyList())?.isEmpty() ?: true
        }
    }

    object Refresh : NewsIntent {
        override fun isValidFor(modelState: NewsModelState): Boolean {
            return PullToRefresh.canRefresh(modelState.lastFreshDataTime)
        }
    }
}
