package com.blockchain.componentlib.button

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.common.BaseButtonView
import com.blockchain.componentlib.button.common.ButtonStyle
import com.blockchain.componentlib.button.common.Button
import com.blockchain.componentlib.button.common.ButtonIconColor
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Plus
import com.blockchain.componentlib.theme.AppColors

class MinimalErrorButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseButtonView(context, attrs, defStyleAttr) {

    @Composable
    override fun Content() {
        MinimalErrorButton(
            onClick = onClick,
            text = text,
            state = buttonState,
            icon = icon as? ImageResource.Local
        )
    }
}

private val bgColorLight = Color(0XFFFFFFFF)
private val bgColorDark = Color(0XFF07080D)
private val bgColor @Composable get() = if (isSystemInDarkTheme()) bgColorDark else bgColorLight

private val borderColorLight = Color(0XFFF0F2F7)
private val borderColorDark = Color(0XFF2C3038)
private val borderColor @Composable get() = if (isSystemInDarkTheme()) borderColorDark else borderColorLight

@Composable
fun MinimalErrorButton(
    modifier: Modifier = Modifier,
    text: String,
    state: ButtonState = ButtonState.Enabled,
    icon: ImageResource.Local? = null,
    iconColor: ButtonIconColor = ButtonIconColor.Default,
    onClick: () -> Unit
) {
    MinimalErrorButton(
        modifier = modifier,
        text = text,
        state = state,
        icon = icon,
        iconColor = iconColor,
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
    iconColor: ButtonIconColor = ButtonIconColor.Default,
    onClick: () -> Unit
) {
    MinimalErrorButton(
        modifier = modifier,
        text = text,
        state = state,
        icon = icon,
        iconColor = iconColor,
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
    iconColor: ButtonIconColor = ButtonIconColor.Default,
    style: ButtonStyle,
    onClick: () -> Unit
) {
    com.blockchain.componentlib.button.common.OutlinedButton(
        modifier = modifier,
        text = text,
        textColor = AppColors.error,
        backgroundColor = bgColor,
        disabledBackgroundColor = bgColor,
        borderColor = borderColor,
        state = state,
        style = style,
        icon = icon,
        iconColor = iconColor,
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
