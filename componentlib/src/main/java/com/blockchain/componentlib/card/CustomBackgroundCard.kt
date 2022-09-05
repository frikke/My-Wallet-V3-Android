package com.blockchain.componentlib.card

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.media.AsyncMediaItem
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark800
import com.blockchain.componentlib.theme.Grey800
import com.blockchain.componentlib.theme.Grey900

@OptIn(ExperimentalCoilApi::class)
@Composable
fun CustomBackgroundCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    iconResource: ImageResource = ImageResource.None,
    backgroundResource: ImageResource = ImageResource.None,
    isCloseable: Boolean = true,
    onClose: () -> Unit = {},
    onClick: () -> Unit = {},
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    textColor: Color = Color.White
) {

    val backgroundColor = if (!isDarkTheme) {
        Grey900
    } else {
        Dark800
    }

    Box(
        modifier = Modifier
            .padding(2.dp)
            .shadow(2.dp, AppTheme.shapes.medium)
            .wrapContentHeight()
            .clickable(onClick = onClick)
            .defaultMinSize(minWidth = 340.dp)
            .background(color = backgroundColor, shape = AppTheme.shapes.medium)
    ) {
        if (backgroundResource is ImageResource.Remote) {
            AsyncMediaItem(
                modifier = modifier
                    .alpha(0.9f)
                    .clipToBounds()
                    .matchParentSize()
                    .align(Alignment.Center),
                url = backgroundResource.url,
                contentDescription = "",
                contentScale = ContentScale.FillWidth
            )
        } else {
            Image(
                modifier = modifier
                    .alpha(0.9f)
                    .clipToBounds()
                    .matchParentSize()
                    .align(Alignment.Center),
                contentScale = ContentScale.FillWidth,
                imageResource = backgroundResource
            )
        }

        Surface(
            modifier = Modifier
                .background(Color.Transparent)
                .padding(
                    start = dimensionResource(R.dimen.medium_margin),
                    end = dimensionResource(R.dimen.medium_margin),
                    top = dimensionResource(R.dimen.very_small_margin),
                    bottom = dimensionResource(R.dimen.very_small_margin)
                ),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .background(Color.Transparent),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                when (iconResource) {
                    is ImageResource.Remote -> {
                        AsyncMediaItem(
                            modifier = Modifier.size(dimensionResource(R.dimen.large_margin)),
                            url = iconResource.url,
                            contentDescription = "",
                            contentScale = ContentScale.Inside,
                        )
                    }
                    is ImageResource.None -> {
                        // do nothing
                    }
                    else -> {
                        Image(
                            modifier = Modifier.size(dimensionResource(R.dimen.large_margin)),
                            contentScale = ContentScale.Inside,
                            imageResource = iconResource
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f, true)
                        .padding(
                            start = if (iconResource !is ImageResource.None) {
                                dimensionResource(R.dimen.medium_margin)
                            } else 0.dp,
                            end = dimensionResource(R.dimen.tiny_margin)
                        )
                        .align(Alignment.Top)
                ) {

                    Text(
                        text = title,
                        style = AppTheme.typography.caption1,
                        color = textColor
                    )

                    Text(
                        text = subtitle,
                        style = AppTheme.typography.paragraph2,
                        color = textColor
                    )
                }

                if (isCloseable) {
                    CardCloseButton(
                        modifier = Modifier.align(Alignment.Top),
                        backgroundColor = Grey800,
                        onClick = onClose
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun CustomBackgroundCard_Non_Closeable() {
    AppTheme {
        AppSurface {
            CustomBackgroundCard(
                title = "Title",
                subtitle = "Subtitle",
                iconResource = ImageResource.Local(R.drawable.ic_star),
                backgroundResource = ImageResource.Local(R.drawable.ic_blockchain_logo_with_text),
                isCloseable = false
            )
        }
    }
}

@Preview
@Composable
fun CustomBackgroundCard_Closeable() {
    AppTheme {
        AppSurface {
            CustomBackgroundCard(
                title = "Title",
                subtitle = "Subtitle",
                iconResource = ImageResource.Local(R.drawable.ic_star),
                backgroundResource = ImageResource.Local(R.drawable.ic_blockchain_logo_with_text),
                isCloseable = true
            )
        }
    }
}

@Preview
@Composable
fun CustomBackgroundCard_Remote() {
    AppTheme {
        AppSurface {
            CustomBackgroundCard(
                title = "Title",
                subtitle = "Subtitle",
                iconResource = ImageResource.Remote(
                    "https://firebasestorage.googleapis.com/v0/b/fir-staging-92d79.appspot.com/o/announcement.png"
                ),
                backgroundResource = ImageResource.Remote(
                    "https://firebasestorage.googleapis.com/v0/b/" +
                        "fir-staging-92d79.appspot.com/o/icon-cowboys-circle.svg?"
                ),
                isCloseable = true
            )
        }
    }
}

@Preview
@Composable
fun CustomBackgroundCard_NoIcon() {
    AppTheme {
        AppSurface {
            CustomBackgroundCard(
                title = "Title",
                subtitle = "Subtitle",
                iconResource = ImageResource.None,
                backgroundResource = ImageResource.Local(R.drawable.ic_blockchain_logo_with_text),
                isCloseable = false
            )
        }
    }
}
