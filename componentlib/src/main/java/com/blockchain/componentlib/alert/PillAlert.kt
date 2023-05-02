package com.blockchain.componentlib.alert

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Alert
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Star
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark800
import com.blockchain.componentlib.theme.Green700
import com.blockchain.componentlib.theme.Red400
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.componentlib.utils.value

@Stable
data class PillAlert(
    val text: TextValue,
    val icon: ImageResource.Local? = null,
    val type: PillAlertType
)

enum class PillAlertType(
    val bgColor: Color,
    val textColor: Color
) {
    Info(bgColor = Dark800, textColor = Color.White),
    Error(bgColor = Dark800, textColor = Red400),
    Success(bgColor = Dark800, textColor = Green700),
    // todo add more types
}

@Composable
fun PillAlert(
    modifier: Modifier = Modifier,
    config: PillAlert
) {
    Row(
        modifier = modifier
            .background(
                color = config.type.bgColor,
                shape = AppTheme.shapes.extraLarge
            )
            .padding(
                horizontal = AppTheme.dimensions.standardSpacing,
                vertical = AppTheme.dimensions.verySmallSpacing
            )
            .clickable(true, onClick = {}),
        verticalAlignment = Alignment.CenterVertically
    ) {

        config.icon?.let { icon ->
            Image(
                imageResource = icon.withSize(AppTheme.dimensions.smallSpacing)
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
        }

        Text(
            text = config.text.value(),
            style = AppTheme.typography.body2,
            color = config.type.textColor
        )
    }
}

@Preview
@Composable
fun PreviewPillAlert_Info() {
    AppTheme {
        AppSurface {
            PillAlert(
                config = PillAlert(
                    text = TextValue.StringValue("Added to favorites"),
                    icon = Icons.Filled.Star.withTint(Color(0XFFFFCD53)),
                    type = PillAlertType.Info
                )
            )
        }
    }
}

@Preview
@Composable
fun PreviewPillAlert_Error() {
    AppTheme {
        AppSurface {
            PillAlert(
                config = PillAlert(
                    text = TextValue.StringValue("USDC approval failed"),
                    icon = Icons.Filled.Alert.withTint(Red400),
                    type = PillAlertType.Error
                )
            )
        }
    }
}
