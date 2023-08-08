package com.blockchain.componentlib.alert

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.componentlib.utils.value

private val bgColorLight = Color(0XFF20242C)
private val bgColorDark = Color(0XFF000000)
private val bgColor @Composable get() = if (isSystemInDarkTheme()) bgColorDark else bgColorLight

@Stable
data class PillAlert(
    val text: TextValue,
    val icon: ImageResource.Local? = null,
    val type: PillAlertType
)

enum class PillAlertType {
    Info,
    Error,
    Success,
    Warning
}

@Composable
fun PillAlertType.color() = when (this) {
    PillAlertType.Info -> Color.White
    PillAlertType.Error -> AppColors.error
    PillAlertType.Success -> AppColors.success
    PillAlertType.Warning -> AppColors.primary
}

@Composable
fun PillAlert(
    modifier: Modifier = Modifier,
    color: Color = bgColor,
    config: PillAlert
) {
    Row(
        modifier = modifier
            .background(
                color = color,
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
            color = config.type.color()
        )
    }
}

@Preview
@Composable
fun PreviewPillAlert_Info() {
    PillAlert(
        config = PillAlert(
            text = TextValue.StringValue("Added to favorites"),
            icon = Icons.Filled.Star.withTint(Color(0XFFFFCD53)),
            type = PillAlertType.Info
        )
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewPillAlert_InfoDark() {
    PreviewPillAlert_Info()
}

@Preview
@Composable
fun PreviewPillAlert_Error() {
    PillAlert(
        config = PillAlert(
            text = TextValue.StringValue("USDC approval failed"),
            icon = Icons.Filled.Alert.withTint(AppColors.error),
            type = PillAlertType.Error
        )
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewPillAlert_ErrorDark() {
    PreviewPillAlert_Error()
}

@Preview
@Composable
fun PreviewPillAlert_Success() {
    PillAlert(
        config = PillAlert(
            text = TextValue.StringValue("USDC approval Successful"),
            icon = Icons.Filled.Alert.withTint(AppColors.success),
            type = PillAlertType.Success
        )
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewPillAlert_SuccessDark() {
    PreviewPillAlert_Success()
}
