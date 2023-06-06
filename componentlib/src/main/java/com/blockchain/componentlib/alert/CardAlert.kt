package com.blockchain.componentlib.alert

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.SmallSecondaryButton
import com.blockchain.componentlib.card.CardButton
import com.blockchain.componentlib.icons.Image
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

enum class AlertType {
    Default, Success, Warning, Error
}

@Composable
fun CardAlert(
    title: String,
    subtitle: String,
    alertType: AlertType = AlertType.Default,
    isBordered: Boolean = true,
    backgroundColor: Color = AppTheme.colors.backgroundSecondary,
    isDismissable: Boolean = true,
    onClose: () -> Unit = {},
    primaryCta: CardButton? = null,
    secondaryCta: CardButton? = null
) {
    val typeColor = when (alertType) {
        AlertType.Default -> AppTheme.colors.title
        AlertType.Success -> AppTheme.colors.success
        AlertType.Warning -> AppTheme.colors.warning
        AlertType.Error -> AppTheme.colors.error
    }

    Surface(
        modifier = Modifier.defaultMinSize(minWidth = 340.dp),
        shape = AppTheme.shapes.large,
        color = backgroundColor,
        border = BorderStroke(1.dp, typeColor).takeIf { isBordered }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing))
        ) {
            Row {
                if (title.isNotEmpty()) {
                    Text(
                        modifier = Modifier
                            .weight(1f, true),
                        text = title,
                        style = AppTheme.typography.body2,
                        color = typeColor
                    )
                }
                if (isDismissable) {
                    CardCloseButton(onClick = onClose)
                }
            }

            if (subtitle.isNotEmpty()) {
                Text(
                    modifier = Modifier
                        .padding(
                            top = if (title.isNotEmpty()) dimensionResource(
                                id = com.blockchain.componentlib.R.dimen.tiny_spacing
                            ) else 0.dp
                        ),
                    text = subtitle,
                    style = AppTheme.typography.paragraph1,
                    color = AppTheme.colors.title
                )
            }

            if (primaryCta != null) {
                Row(
                    modifier = Modifier
                        .padding(top = dimensionResource(id = com.blockchain.componentlib.R.dimen.small_spacing))
                ) {
                    SmallSecondaryButton(
                        text = primaryCta.text,
                        onClick = primaryCta.onClick,
                        state = ButtonState.Enabled
                    )

                    if (secondaryCta != null) {
                        Spacer(
                            modifier = Modifier.size(
                                size = dimensionResource(id = com.blockchain.componentlib.R.dimen.tiny_spacing)
                            )
                        )

                        SmallSecondaryButton(
                            text = secondaryCta.text,
                            onClick = secondaryCta.onClick,
                            state = ButtonState.Enabled
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CardCloseButton(
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .clickable {
                onClick.invoke()
            }
            .size(dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing))
    ) {
        Image(
            imageResource = ImageResource.Local(R.drawable.ic_close_circle),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Preview
@Composable
fun DefaultCardAlert_Basic() {
    AppTheme {
        AppSurface {
            CardAlert(title = "Title", subtitle = "Subtitle")
        }
    }
}

@Preview
@Composable
fun SuccessCardAlert_Basic() {
    AppTheme {
        AppSurface {
            CardAlert(title = "Title", subtitle = "Subtitle", alertType = AlertType.Success)
        }
    }
}

@Preview
@Composable
fun SuccessCardAlert_OneButton() {
    AppTheme {
        AppSurface {
            CardAlert(
                title = "Title",
                subtitle = "Subtitle",
                alertType = AlertType.Default,
                primaryCta = CardButton(
                    text = "Primary button",
                    onClick = {}
                )
            )
        }
    }
}

@Preview
@Composable
fun SuccessCardAlert_TwoButtons() {
    AppTheme {
        AppSurface {
            CardAlert(
                title = "Title",
                subtitle = "Subtitle",
                alertType = AlertType.Success,
                primaryCta = CardButton(
                    text = "Primary button",
                    onClick = {}
                ),
                secondaryCta = CardButton(
                    text = "Secondary button",
                    onClick = {}
                )
            )
        }
    }
}

@Preview
@Composable
fun SuccessCardAlert_NoTitle() {
    AppTheme {
        AppSurface {
            CardAlert(
                title = "",
                subtitle = "Subtitle",
                alertType = AlertType.Default,
                primaryCta = CardButton(
                    text = "Primary button",
                    onClick = {}
                ),
                isDismissable = false
            )
        }
    }
}
