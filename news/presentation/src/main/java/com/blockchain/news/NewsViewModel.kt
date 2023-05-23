package com.blockchain.news

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.EmptyNavEvent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.mapData
import com.blockchain.data.updateDataWith
import com.blockchain.presentation.pulltorefresh.PullToRefresh
import com.blockchain.utils.CurrentTimeProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NewsViewModel(
    private val newsService: NewsService
) : MviViewModel<
    NewsIntent,
    NewsViewState,
    NewsModelState,
    EmptyNavEvent,
    ModelConfigArgs.NoArgs
    >(
    NewsModelState()
) {
    private var newsJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun NewsModelState.reduce() = NewsViewState(
        newsArticles = newsArticles
    )

    override suspend fun handleIntent(modelState: NewsModelState, intent: NewsIntent) {
        when (intent) {
            is NewsIntent.LoadData -> {
                loadNews()
            }

            NewsIntent.Refresh -> {
                updateState {
                    copy(lastFreshDataTime = CurrentTimeProvider.currentTimeMillis())
                }

                loadNews(forceRefresh = true)
            }
        }
    }

    private fun loadNews(forceRefresh: Boolean = false) {
        newsJob?.cancel()
        newsJob = viewModelScope.launch {
            val forTicker = newsService.preferredNewsAssetTickers()
            newsService.articles(
                freshnessStrategy = PullToRefresh.freshnessStrategy(
                    shouldGetFresh = forceRefresh,
                    cacheStrategy = RefreshStrategy.RefreshIfStale
                ),
                tickers = forTicker
            ).mapData {
                it.take(MAX_NEWS_COUNT)
            }.collectLatest { newsArticlesResource ->
                updateState {
                    copy(newsArticles = newsArticles.updateDataWith(newsArticlesResource))
                }
            }
        }
    }

    companion object {
        private const val MAX_NEWS_COUNT = 20
    }
}
