package com.blockchain.news.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.news.NewsArticle
import com.blockchain.utils.toFormattedDate
import java.util.Date

@Composable
fun NewsArticle(
    modifier: Modifier = Modifier,
    newsArticle: NewsArticle
) {
    Surface(
        modifier = modifier,
        shape = AppTheme.shapes.large,
        color = AppTheme.colors.backgroundSecondary,
    ) {
        val context = LocalContext.current
        Row(
            modifier = Modifier
                .clickable(
                    onClick = { context.openUrl(newsArticle.link) }
                )
                .fillMaxWidth()
                .padding(AppTheme.dimensions.smallSpacing)
        ) {
            Column(
                modifier = Modifier.weight(1F)
            ) {
                Text(
                    text = newsArticle.title,
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                newsArticle.date?.let {
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

                    Text(
                        text = it.toFormattedDate(),
                        style = AppTheme.typography.paragraph1,
                        color = AppTheme.colors.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                newsArticle.author?.let {
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

                    Text(
                        text = stringResource(com.blockchain.stringResources.R.string.news_author, it),
                        style = AppTheme.typography.paragraph1,
                        color = AppTheme.colors.body,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            newsArticle.image?.let {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

                Image(
                    imageResource =
                    ImageResource.Remote(
                        url = it,
                        size = 64.dp
                    ),
                    defaultShape = AppTheme.shapes.large,
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewNewsArticle() {
    NewsArticle(
        newsArticle = NewsArticle(
            id = 0,
            title = "Ethereum core developers plan new testnet called Holli Ethereum core developers plan new testnet",
            image = "",
            date = Date(),
            author = "author",
            link = "",
        )
    )
}

@Preview
@Composable
private fun PreviewNewsArticle_NoImage() {
    NewsArticle(
        newsArticle = NewsArticle(
            id = 0,
            title = "Ethereum core developers plan new testnet called Holli Ethereum core developers plan new testnet",
            image = null,
            date = Date(),
            author = "author",
            link = "",
        )
    )
}
