package com.blockchain.news.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.news.NewsArticle
import com.blockchain.news.NewsIntent
import com.blockchain.news.NewsViewModel
import com.blockchain.news.NewsViewState
import com.blockchain.stringResources.R
import org.koin.androidx.compose.getViewModel

@Composable
fun NewsArticlesScreen(
    viewModel: NewsViewModel = getViewModel(),
    onBackPressed: () -> Unit
) {
    val viewState: NewsViewState by viewModel.viewState.collectAsStateLifecycleAware()

    LaunchedEffect(key1 = viewModel) {
        viewModel.onIntent(NewsIntent.LoadData)
    }

    NewsArticles(
        newsArticles = viewState.newsArticles,
        onBackPressed = onBackPressed
    )
}

@Composable
private fun NewsArticles(
    newsArticles: DataResource<List<NewsArticle>>,
    onBackPressed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0XFFF1F2F7))
    ) {
        NavigationBar(
            title = stringResource(R.string.news_title),
            onBackButtonClick = onBackPressed,
        )

        when (newsArticles) {
            is DataResource.Loading -> {
                ShimmerLoadingCard()
            }

            is DataResource.Error -> {
            }

            is DataResource.Data -> {
                LazyColumn(
                    contentPadding = PaddingValues(AppTheme.dimensions.smallSpacing),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.smallSpacing)
                ) {
                    items(newsArticles.data) {
                        NewsArticle(
                            newsArticle = it
                        )
                    }
                }
            }
        }
    }
}