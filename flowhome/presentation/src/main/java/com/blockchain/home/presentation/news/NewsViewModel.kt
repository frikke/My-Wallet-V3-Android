package com.blockchain.home.presentation.news

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.EmptyNavEvent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.dataOrElse
import com.blockchain.data.mapData
import com.blockchain.data.updateDataWith
import com.blockchain.news.NewsService
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

    override fun reduce(state: NewsModelState) = state.run {
        NewsViewState(
            newsArticles = newsArticles.dataOrElse(emptyList())
        )
    }

    override suspend fun handleIntent(modelState: NewsModelState, intent: NewsIntent) {
        when (intent) {
            is NewsIntent.LoadData -> {
                loadNews()
            }

            NewsIntent.Refresh -> {
                updateState {
                    it.copy(lastFreshDataTime = CurrentTimeProvider.currentTimeMillis())
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
                tickers = forTicker)
                .mapData { it.take(MAX_NEWS_COUNT) }
                .collectLatest { newsArticles ->
                    println("------ newss ${newsArticles}")

                    updateState {
                        it.copy(newsArticles = it.newsArticles.updateDataWith(newsArticles))
                    }
                }
        }
    }

    companion object {
        private const val MAX_NEWS_COUNT = 5
    }
}