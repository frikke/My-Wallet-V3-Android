package com.blockchain.componentlib.alert

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
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
import com.blockchain.componentlib.basic.CloseIcon
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.SecondarySmallButton
import com.blockchain.componentlib.card.CardButton
import com.blockchain.componentlib.icons.Copy
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.clickableNoEffect

enum class AlertType {
    Default, Success, Warning, Error
}

@Composable
fun CardAlert(
    title: String,
    subtitle: String,
    titleIcon: ImageResource.Local? = null,
    alertType: AlertType = AlertType.Default,
    isBordered: Boolean = true,
    backgroundColor: Color = AppTheme.colors.backgroundSecondary,
    isDismissable: Boolean = true,
    titleIconOnClick: () -> Unit = {},
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
                    Row(
                        modifier = Modifier.weight(1f, true),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = AppTheme.typography.body2,
                            color = typeColor
                        )

                        titleIcon?.let {
                            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
                            Image(
                                modifier = Modifier.clickableNoEffect(onClick = titleIconOnClick),
                                imageResource = titleIcon
                                    .withTint(AppColors.title)
                                    .withSize(AppTheme.dimensions.smallSpacing)
                            )
                        }
                    }
                }
                if (isDismissable) {
                    CloseIcon(onClick = onClose)
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
                    SecondarySmallButton(
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

                        SecondarySmallButton(
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

@Preview
@Composable
private fun DefaultCardAlert_Basic() {
    CardAlert(title = "Title", subtitle = "Subtitle", titleIcon = Icons.Filled.Copy)
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DefaultCardAlert_BasicDark() {
    DefaultCardAlert_Basic()
}

@Preview
@Composable
fun SuccessCardAlert_Basic() {
    CardAlert(title = "Title", subtitle = "Subtitle", alertType = AlertType.Success)
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SuccessCardAlert_BasicDark() {
    SuccessCardAlert_Basic()
}

@Preview
@Composable
fun SuccessCardAlert_OneButton() {
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

@Preview
@Composable
fun SuccessCardAlert_TwoButtons() {
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

@Preview
@Composable
fun SuccessCardAlert_NoTitle() {
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
