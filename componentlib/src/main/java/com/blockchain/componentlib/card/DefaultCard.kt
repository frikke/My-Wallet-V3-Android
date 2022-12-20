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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.SecondaryButton
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
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    isDismissable: Boolean = true,
) {

    val backgroundColor = if (!isDarkTheme) {
        Color.White
    } else {
        Dark800
    }

    Box(
        modifier = Modifier
            .padding(2.dp)
            .defaultMinSize(minWidth = 340.dp)
            .background(color = backgroundColor, shape = AppTheme.shapes.medium)
    ) {
        Surface(
            modifier = Modifier
                .padding(dimensionResource(R.dimen.medium_spacing))
                .background(backgroundColor)
                .clip(AppTheme.shapes.small)

        ) {
            Column(
                modifier = Modifier.background(backgroundColor)
            ) {
                Row {
                    Image(
                        modifier = Modifier
                            .size(dimensionResource(R.dimen.large_spacing)),
                        imageResource = iconResource
                    )
                    Spacer(
                        modifier = Modifier.weight(1f, true)
                    )

                    if (isDismissable) {
                        CardCloseButton(onClick = onClose)
                    }
                }

                Text(
                    modifier = Modifier
                        .padding(top = dimensionResource(R.dimen.very_small_spacing)),
                    text = title,
                    style = AppTheme.typography.title3,
                    color = AppTheme.colors.title
                )

                Text(
                    modifier = Modifier
                        .padding(top = dimensionResource(R.dimen.tiny_spacing)),
                    text = subtitle,
                    style = AppTheme.typography.paragraph1,
                    color = AppTheme.colors.title
                )

                if (callToActionButton != null) {
                    when (callToActionButton.type) {
                        ButtonType.Primary -> PrimaryButton(
                            modifier = Modifier
                                .padding(top = dimensionResource(R.dimen.very_small_spacing))
                                .fillMaxWidth(),
                            defaultBackgroundColor = callToActionButton.backgroundColor,
                            text = callToActionButton.text,
                            onClick = callToActionButton.onClick,
                            state = ButtonState.Enabled
                        )
                        ButtonType.Secondary -> SecondaryButton(
                            modifier = Modifier
                                .padding(top = dimensionResource(R.dimen.very_small_spacing))
                                .fillMaxWidth(),
                            text = callToActionButton.text,
                            onClick = callToActionButton.onClick,
                            state = ButtonState.Enabled
                        )
                        ButtonType.Minimal -> MinimalButton(
                            modifier = Modifier
                                .padding(top = dimensionResource(R.dimen.very_small_spacing))
                                .fillMaxWidth(),
                            text = callToActionButton.text,
                            onClick = callToActionButton.onClick,
                            state = ButtonState.Enabled
                        )
                    }
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

@Preview
@Composable
fun DefaultCardAlert_Secondary_Button() {
    AppTheme {
        AppSurface {
            DefaultCard(
                title = "Title", subtitle = "Subtitle",
                iconResource = ImageResource.Local(R.drawable.ic_blockchain, null),
                CardButton("Notify Me", type = ButtonType.Secondary) {}
            )
        }
    }
}

@Preview
@Composable
fun DefaultCardAlert_Minimal_Button() {
    AppTheme {
        AppSurface {
            DefaultCard(
                title = "Title", subtitle = "Subtitle",
                iconResource = ImageResource.Local(R.drawable.ic_blockchain, null),
                CardButton("Notify Me", type = ButtonType.Minimal) {}
            )
        }
    }
}
