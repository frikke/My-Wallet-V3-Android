package com.blockchain.componentlib.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark800

@Composable
fun CtaAnnouncementCard(
    header: String,
    subheader: AnnotatedString,
    title: String,
    body: String,
    iconResource: ImageResource = ImageResource.None,
    borderColor: Color? = null,
    callToActionButton: CardButton,
    onClose: () -> Unit = {},
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {

    val backgroundColor = if (!isDarkTheme) {
        Color.White
    } else {
        Dark800
    }

    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 340.dp)
            .background(color = backgroundColor, shape = AppTheme.shapes.medium)
            .border(1.dp, borderColor ?: Color.Transparent, AppTheme.shapes.medium)
    ) {
        Surface(
            modifier = Modifier
                .padding(dimensionResource(R.dimen.medium_margin))
        ) {
            Column(
                modifier = Modifier
                    .background(backgroundColor)
            ) {
                Row(
                    modifier = Modifier
                        .background(backgroundColor),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        modifier = Modifier
                            .size(dimensionResource(R.dimen.large_margin)),
                        imageResource = iconResource
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f, true)
                            .padding(start = 8.dp, end = 8.dp)
                            .background(backgroundColor)
                    ) {

                        Text(
                            text = header,
                            style = AppTheme.typography.caption2,
                            color = AppTheme.colors.title
                        )

                        Text(
                            text = subheader,
                            style = AppTheme.typography.caption2,
                            color = AppTheme.colors.title
                        )
                    }

                    CardCloseButton(
                        modifier = Modifier.align(Alignment.Top),
                        onClick = onClose
                    )
                }

                Text(
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.medium_margin)),
                    text = title,
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.title
                )

                Text(
                    text = body,
                    style = AppTheme.typography.paragraph1,
                    color = AppTheme.colors.title
                )

                PrimaryButton(
                    modifier = Modifier
                        .padding(top = dimensionResource(R.dimen.medium_margin))
                        .fillMaxWidth(),
                    defaultBackgroundColor = callToActionButton.backgroundColor,
                    text = callToActionButton.text,
                    onClick = callToActionButton.onClick,
                    state = ButtonState.Enabled
                )
            }
        }
    }
}

@Preview
@Composable
fun CtaAnnouncementCard_Basic() {
    AppTheme {
        AppSurface {
            CtaAnnouncementCard(
                header = "Title",
                subheader = AnnotatedString("Subtitle"),
                title = "Uniswap (UNI) is Now Trading",
                body = "Exchange, deposit, withdraw, or store UNI in your Blockchain.com Exchange account.",
                iconResource = ImageResource.Local(R.drawable.ic_blockchain, null),
                callToActionButton = CardButton("Trade UNI", Color(0xFFFF007A)) {}
            )
        }
    }
}
