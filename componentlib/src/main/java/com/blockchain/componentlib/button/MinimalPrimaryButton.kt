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
fun MinimalPrimaryButton(
    modifier: Modifier = Modifier,
    text: String,
    state: ButtonState = ButtonState.Enabled,
    icon: ImageResource.Local? = null,
    onClick: () -> Unit
) {
    MinimalPrimaryButton(
        modifier = modifier,
        text = text,
        state = state,
        icon = icon,
        style = ButtonStyle.Default,
        onClick = onClick
    )
}

@Composable
fun MinimalPrimarySmallButton(
    modifier: Modifier = Modifier,
    text: String,
    state: ButtonState = ButtonState.Enabled,
    icon: ImageResource.Local? = null,
    onClick: () -> Unit
) {
    MinimalPrimaryButton(
        modifier = modifier,
        text = text,
        state = state,
        icon = icon,
        style = ButtonStyle.Small,
        onClick = onClick
    )
}

@Composable
private fun MinimalPrimaryButton(
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
private fun PreviewButton() {
    MinimalPrimaryButton(
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
    MinimalPrimarySmallButton(
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
    MinimalPrimaryButton(
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
    MinimalPrimarySmallButton(
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
    MinimalPrimaryButton(
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
    MinimalPrimarySmallButton(
        text = "Button Text", state = ButtonState.Loading, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewButtonSmallLoadingDark() {
    PreviewButtonSmallLoading()
}
