package com.blockchain.componentlib.card

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
fun DefaultCard(
    title: String,
    subtitle: String,
    iconResource: ImageResource = ImageResource.None,
    callToActionButton: CardButton? = null,
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
            .padding(2.dp)
            .shadow(2.dp, AppTheme.shapes.medium)
            .defaultMinSize(minWidth = 340.dp)
            .background(color = backgroundColor, shape = AppTheme.shapes.medium)
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .background(backgroundColor)
                .clip(AppTheme.shapes.small)

        ) {
            Column(
                modifier = Modifier.background(backgroundColor)
            ) {
                Row {
                    Image(
                        modifier = Modifier
                            .size(32.dp),
                        imageResource = iconResource
                    )
                    Spacer(
                        modifier = Modifier.weight(1f, true)
                    )
                    CardCloseButton(onClick = onClose)
                }

                Text(
                    modifier = Modifier
                        .padding(top = 23.dp),
                    text = title,
                    style = AppTheme.typography.title3,
                    color = AppTheme.colors.title
                )

                Text(
                    modifier = Modifier
                        .padding(top = 8.dp),
                    text = subtitle,
                    style = AppTheme.typography.paragraph1,
                    color = AppTheme.colors.title
                )

                if (callToActionButton != null) {
                    PrimaryButton(
                        modifier = Modifier
                            .padding(top = 16.dp)
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
}

@Preview
@Composable
fun DefaultCardAlert_Basic() {
    AppTheme {
        AppSurface {
            DefaultCard(
                title = "Title", subtitle = "Subtitle",
                iconResource = ImageResource.Local(R.drawable.ic_blockchain, null),
                CardButton("Notify Me") {}
            )
        }
    }
}
