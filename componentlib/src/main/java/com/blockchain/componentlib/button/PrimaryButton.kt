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
import com.blockchain.componentlib.button.common.Button
import com.blockchain.componentlib.button.common.ButtonIconColor
import com.blockchain.componentlib.button.common.ButtonStyle
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Plus

class PrimaryButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseButtonView(context, attrs, defStyleAttr) {

    @Composable
    override fun Content() {
        if (isInEditMode) {
            text = "dummy text"
        }

        PrimaryButton(
            onClick = onClick,
            text = text,
            state = buttonState,
            icon = icon as? ImageResource.Local
        )
    }
}

private val bgColorLight = Color(0XFF0C6CF2)
private val bgColorDark = Color(0XFF0C6CF2)
private val bgColor @Composable get() = if (isSystemInDarkTheme()) bgColorDark else bgColorLight

private val disabledBgColorLight = Color(0XFF65A5FF)
private val disabledBgColorDark = Color(0XFF1656B9)
private val disabledBgColor @Composable get() = if (isSystemInDarkTheme()) disabledBgColorDark else disabledBgColorLight

private val textColorLight = Color(0XFFFFFFFF)
private val textColorDark = Color(0XFF07080D)
private val textColor @Composable get() = if (isSystemInDarkTheme()) textColorDark else textColorLight

@Composable
fun PrimaryButton(
    modifier: Modifier = Modifier,
    text: String,
    state: ButtonState = ButtonState.Enabled,
    icon: ImageResource.Local? = null,
    iconColor: ButtonIconColor = ButtonIconColor.Default,
    onClick: () -> Unit
) {
    PrimaryButton(
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
fun PrimarySmallButton(
    modifier: Modifier = Modifier,
    text: String,
    state: ButtonState = ButtonState.Enabled,
    icon: ImageResource.Local? = null,
    iconColor: ButtonIconColor = ButtonIconColor.Default,
    onClick: () -> Unit
) {
    PrimaryButton(
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
private fun PrimaryButton(
    modifier: Modifier = Modifier,
    text: String,
    state: ButtonState = ButtonState.Enabled,
    icon: ImageResource.Local? = null,
    iconColor: ButtonIconColor = ButtonIconColor.Default,
    style: ButtonStyle,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier,
        text = text,
        textColor = textColor,
        backgroundColor = bgColor,
        disabledBackgroundColor = disabledBgColor,
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
private fun PreviewPrimaryButton() {
    PrimaryButton(
        text = "Button Text", state = ButtonState.Enabled, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewPrimaryButtonDark() {
    PreviewPrimaryButton()
}

@Preview
@Composable
private fun PreviewPrimaryButtonSmall() {
    PrimarySmallButton(
        text = "Button Text", state = ButtonState.Enabled, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewPrimaryButtonSmallDark() {
    PreviewPrimaryButtonSmall()
}

@Preview
@Composable
private fun PreviewPrimaryButtonDisabled() {
    PrimaryButton(
        text = "Button Text", state = ButtonState.Disabled, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewPrimaryButtonDisabledDark() {
    PreviewPrimaryButtonDisabled()
}

@Preview
@Composable
private fun PreviewPrimaryButtonSmallDisabled() {
    PrimarySmallButton(
        text = "Button Text", state = ButtonState.Disabled, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewPrimaryButtonSmallDisabledDark() {
    PreviewPrimaryButtonSmallDisabled()
}

@Preview
@Composable
private fun PreviewPrimaryButtonLoading() {
    PrimaryButton(
        text = "Button Text", state = ButtonState.Loading, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewPrimaryButtonLoadingDark() {
    PreviewPrimaryButtonLoading()
}

@Preview
@Composable
private fun PreviewPrimaryButtonSmallLoading() {
    PrimarySmallButton(
        text = "Button Text", state = ButtonState.Loading, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewPrimaryButtonSmallLoadingDark() {
    PreviewPrimaryButtonSmallLoading()
}
