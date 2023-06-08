package com.blockchain.componentlib.button

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.common.ButtonStyle
import com.blockchain.componentlib.button.common.FilledButton
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Plus
import com.blockchain.componentlib.theme.AppColors

@Composable
fun MinimalErrorButton(
    modifier: Modifier = Modifier,
    text: String,
    state: ButtonState = ButtonState.Enabled,
    icon: ImageResource.Local? = null,
    onClick: () -> Unit
) {
    MinimalErrorButton(
        modifier = modifier,
        text = text,
        state = state,
        icon = icon,
        style = ButtonStyle.Default,
        onClick = onClick
    )
}

@Composable
fun MinimalErrorSmallButton(
    modifier: Modifier = Modifier,
    text: String,
    state: ButtonState = ButtonState.Enabled,
    icon: ImageResource.Local? = null,
    onClick: () -> Unit
) {
    MinimalErrorButton(
        modifier = modifier,
        text = text,
        state = state,
        icon = icon,
        style = ButtonStyle.Small,
        onClick = onClick
    )
}

@Composable
private fun MinimalErrorButton(
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
        textColor = AppColors.error,
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
private fun PreviewButton() {
    MinimalErrorButton(
        text = "Button Text", state = ButtonState.Enabled, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewButtonDark() {
    PreviewButton()
}

@Preview
@Composable
private fun PreviewButtonSmall() {
    MinimalErrorSmallButton(
        text = "Button Text", state = ButtonState.Enabled, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewButtonSmallDark() {
    PreviewButtonSmall()
}

@Preview
@Composable
private fun PreviewButtonDisabled() {
    MinimalErrorButton(
        text = "Button Text", state = ButtonState.Disabled, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewButtonDisabledDark() {
    PreviewButtonDisabled()
}

@Preview
@Composable
private fun PreviewButtonSmallDisabled() {
    MinimalErrorSmallButton(
        text = "Button Text", state = ButtonState.Disabled, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewButtonSmallDisabledDark() {
    PreviewButtonSmallDisabled()
}

@Preview
@Composable
private fun PreviewButtonLoading() {
    MinimalErrorButton(
        text = "Button Text", state = ButtonState.Loading, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewButtonLoadingDark() {
    PreviewButtonLoading()
}

@Preview
@Composable
private fun PreviewButtonSmallLoading() {
    MinimalErrorSmallButton(
        text = "Button Text", state = ButtonState.Loading, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewButtonSmallLoadingDark() {
    PreviewButtonSmallLoading()
}
