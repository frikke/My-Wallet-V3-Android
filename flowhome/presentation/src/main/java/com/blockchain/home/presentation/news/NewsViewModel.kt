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
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NewsViewModel(
    private val walletModeService: WalletModeService,
    private val newsService: NewsService,
    private val dispatcher: CoroutineDispatcher,
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

    init {
        viewModelScope.launch {
            walletModeService.availableModes().let {
                updateState {
                    copy(availableModes = it)
                }
            }
        }
    }

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun NewsModelState.reduce() = NewsViewState(
        newsArticles = if (walletMode?.isEligible() == true) {
            newsArticles.dataOrElse(emptyList())
        } else {
            null
        }
    )

    override suspend fun handleIntent(modelState: NewsModelState, intent: NewsIntent) {
        when (intent) {
            is NewsIntent.LoadData -> {
                updateState {
                    copy(walletMode = intent.walletMode)
                }
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
        newsJob = viewModelScope.launch(dispatcher) {
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

    /**
     * news is always on custodial
     * is only on defi if custodial mode is disabled
     */
    private fun WalletMode.isEligible(): Boolean {
        return when (this) {
            WalletMode.CUSTODIAL -> true
            WalletMode.NON_CUSTODIAL -> !modelState.availableModes.contains(WalletMode.CUSTODIAL)
        }
    }

    companion object {
        private const val MAX_NEWS_COUNT = 5
    }
}
