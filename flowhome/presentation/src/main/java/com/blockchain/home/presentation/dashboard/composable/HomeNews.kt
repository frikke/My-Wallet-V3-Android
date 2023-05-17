package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.tablerow.TableRowHeader
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.news.composable.NewsArticle
import kotlinx.collections.immutable.ImmutableList

internal fun LazyListScope.homeNews(
    data: ImmutableList<NewsArticle>?,
    seeAllOnClick: () -> Unit
) {
    data?.takeIf { it.isNotEmpty() }?.let { newsArticles ->
        paddedItem(
            paddingValues = PaddingValues(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))
            TableRowHeader(
                title = stringResource(com.blockchain.stringResources.R.string.news_home_title),
                actionTitle = stringResource(com.blockchain.stringResources.R.string.see_all),
                actionOnClick = seeAllOnClick
            )
            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
        }

        itemsIndexed(
            newsArticles
        ) { index, article ->
            NewsArticle(
                modifier = Modifier.padding(horizontal = AppTheme.dimensions.smallSpacing),
                newsArticle = article
            )

            if (newsArticles.lastIndex > index) {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
            }
        }
    }
}
