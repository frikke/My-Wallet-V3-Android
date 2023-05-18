package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.componentlib.button.SmallMinimalButton
import com.blockchain.componentlib.expandables.ExpandableItem
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.tablerow.TableRowHeader
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.previewAnalytics
import com.blockchain.news.NewsArticle
import com.blockchain.news.composable.NewsArticle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.get
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAssetInfoState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewNewsState
import piuk.blockchain.android.ui.dashboard.coinview.CoinViewAnalytics

@Composable
fun News(
    data: CoinviewNewsState,
    newsArticleOnClick: () -> Unit
) {
    data.newsArticles?.takeIf { it.isNotEmpty() }?.let {
        NewsData(
            newsArticles = it.toImmutableList(),
            newsArticleOnClick = newsArticleOnClick
        )
    }
}

@Composable
private fun NewsData(
    newsArticles: ImmutableList<NewsArticle>,
    newsArticleOnClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(AppTheme.dimensions.smallSpacing)
    ) {
        TableRowHeader(
            title = stringResource(com.blockchain.stringResources.R.string.news_home_title)
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        newsArticles.forEachIndexed { index, article ->
            NewsArticle(
                newsArticle = article
            )

            if (newsArticles.lastIndex > index) {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
            }
        }
    }
}