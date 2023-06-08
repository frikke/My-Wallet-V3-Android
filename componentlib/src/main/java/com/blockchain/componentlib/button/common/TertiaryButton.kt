package com.blockchain.componentlib.button.common

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonContent
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Plus
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue000

@Composable
fun MinimalButton(
    modifier: Modifier = Modifier,
    text: String,
    state: ButtonState = ButtonState.Enabled,
    icon: ImageResource.Local? = null,
    onClick: () -> Unit
) {
    MinimalButton(
        modifier = modifier,
        text = text,
        state = state,
        icon = icon,
        style = ButtonStyle.Default,
        onClick = onClick
    )
}

@Composable
fun SmallMinimalButton(
    modifier: Modifier = Modifier,
    text: String,
    state: ButtonState = ButtonState.Enabled,
    icon: ImageResource.Local? = null,
    onClick: () -> Unit
) {
    MinimalButton(
        modifier = modifier,
        text = text,
        state = state,
        icon = icon,
        style = ButtonStyle.Small,
        onClick = onClick
    )
}

@Composable
private fun MinimalButton(
    modifier: Modifier = Modifier,
    text: String,
    state: ButtonState = ButtonState.Enabled,
    icon: ImageResource.Local? = null,
    style: ButtonStyle,
    onClick: () -> Unit
) {
    FilledButton(
        modifier = modifier,
        text = text,
        textColor = AppColors.primary,
        backgroundColor = AppColors.backgroundSecondary,
        disabledBackgroundColor = AppColors.backgroundSecondary,
        state = state,
        style = style,
        icon = icon,
        onClick = onClick
    )
}

// ------------ preview

@Preview
@Composable
private fun PreviewMinimalButton() {
    MinimalButton(
        text = "Button Text", state = ButtonState.Enabled, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMinimalButtonDark() {
    PreviewMinimalButton()
}

@Preview
@Composable
private fun PreviewMinimalButtonSmall() {
    SmallMinimalButton(
        text = "Button Text", state = ButtonState.Enabled, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMinimalButtonSmallDark() {
    PreviewMinimalButtonSmall()
}

@Preview
@Composable
private fun PreviewMinimalButtonDisabled() {
    MinimalButton(
        text = "Button Text", state = ButtonState.Disabled, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMinimalButtonDisabledDark() {
    PreviewMinimalButtonDisabled()
}

@Preview
@Composable
private fun PreviewMinimalButtonSmallDisabled() {
    SmallMinimalButton(
        text = "Button Text", state = ButtonState.Disabled, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMinimalButtonSmallDisabledDark() {
    PreviewMinimalButtonSmallDisabled()
}

@Preview
@Composable
private fun PreviewMinimalButtonLoading() {
    MinimalButton(
        text = "Button Text", state = ButtonState.Loading, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMinimalButtonLoadingDark() {
    PreviewMinimalButtonLoading()
}

@Preview
@Composable
private fun PreviewMinimalButtonSmallLoading() {
    SmallMinimalButton(
        text = "Button Text", state = ButtonState.Loading, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMinimalButtonSmallLoadingDark() {
    PreviewMinimalButtonSmallLoading()
}
