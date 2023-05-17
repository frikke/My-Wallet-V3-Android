package com.blockchain.news

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun NewsArticle(
    newsArticle: NewsArticle
) {
    Surface(
        shape = AppTheme.shapes.large,
        color = AppTheme.colors.background,
    ) {
        Row(
            modifier = Modifier
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

                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

                Text(
                    text = newsArticle.date,
                    style = AppTheme.typography.paragraph1,
                    color = AppTheme.colors.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

                Text(
                    text = stringResource(com.blockchain.stringResources.R.string.news_author, newsArticle.author),
                    style = AppTheme.typography.paragraph1,
                    color = AppTheme.colors.body,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

            Image(
                imageResource = ImageResource.Remote(
                    url = newsArticle.image,
                    size = 64.dp
                ),
                defaultShape = AppTheme.shapes.large,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewNewsArticle() {
    NewsArticle(
        NewsArticle(
            id = 0,
            title = "Ethereum core developers plan new testnet called Holli Ethereum core developers plan new testnet",
            image = "",
            date = "Feb 24, 2023",
            author = "author",
            link = "",
        )
    )
}