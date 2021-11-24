package com.blockchain.componentlib.card

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.image.Image
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark800
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.theme.Grey800
import com.blockchain.componentlib.theme.Grey900

@Composable
fun AnnouncementCard(
    title: String,
    subtitle: String,
    iconResource: ImageResource = ImageResource.None,
    onClose: () -> Unit = {},
    isDarkTheme: Boolean = isSystemInDarkTheme()
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
            .defaultMinSize(minWidth = 340.dp)
            .background(color = backgroundColor, shape = AppTheme.shapes.medium)
    ) {
        androidx.compose.foundation.Image(
            modifier = Modifier
                .alpha(0.05f)
                .clipToBounds()
                .matchParentSize()
                .padding(bottom = 25.dp)
                .align(Alignment.BottomCenter),
            contentScale = ContentScale.Crop,
            painter = painterResource(id = R.drawable.illustration_circuit),
            contentDescription = null
        )
        Surface(
            modifier = Modifier
                .background(Color.Transparent)
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .background(Color.Transparent),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier
                        .size(32.dp),
                    imageResource = iconResource
                )

                Column(
                    modifier = Modifier
                        .weight(1f, true)
                        .padding(start = 16.dp, end = 8.dp)
                        .align(Alignment.Top)
                ) {

                    Text(
                        text = title,
                        style = AppTheme.typography.caption1,
                        color = Grey100
                    )

                    Text(
                        text = subtitle,
                        style = AppTheme.typography.paragraph2,
                        color = Color.White
                    )
                }

                CardCloseButton(
                    modifier = Modifier.align(Alignment.Top),
                    backgroundColor = Grey800,
                    onClick = onClose
                )
            }
        }
    }
}

@Preview
@Composable
fun AnnouncementCard_Basic() {
    AnnotatedString("")
    AppTheme {
        AppSurface {
            AnnouncementCard(
                title = "Title", subtitle = "Subtitle",
                iconResource = ImageResource.Local(R.drawable.ic_blockchain, null)
            )
        }
    }
}
