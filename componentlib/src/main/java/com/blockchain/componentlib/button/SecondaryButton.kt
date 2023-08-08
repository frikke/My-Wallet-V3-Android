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
import com.blockchain.componentlib.theme.AppColors

class SecondaryButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseButtonView(context, attrs, defStyleAttr) {

    @Composable
    override fun Content() {
        if (isInEditMode) {
            text = "dummy text"
        }

        SecondaryButton(
            onClick = onClick,
            text = text,
            state = buttonState
        )
    }
}

class SmallSecondaryButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseButtonView(context, attrs, defStyleAttr) {

    @Composable
    override fun Content() {
        if (isInEditMode) {
            text = "dummy text"
        }

        SecondarySmallButton(
            onClick = onClick,
            text = text,
            state = buttonState
        )
    }
}

private val bgColorLight = Color(0XFF353F52)
private val bgColorDark = Color(0XFF677184)
private val bgColor @Composable get() = if (isSystemInDarkTheme()) bgColorDark else bgColorLight

private val disabledBgColorLight = Color(0XFF828B9E)
private val disabledBgColorDark = Color(0XFF50596B)
private val disabledBgColor @Composable get() = if (isSystemInDarkTheme()) disabledBgColorDark else disabledBgColorLight

@Composable
fun SecondaryButton(
    modifier: Modifier = Modifier,
    text: String,
    state: ButtonState = ButtonState.Enabled,
    icon: ImageResource.Local? = null,
    iconColor: ButtonIconColor = ButtonIconColor.Default,
    onClick: () -> Unit
) {
    SecondaryButton(
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
fun SecondarySmallButton(
    modifier: Modifier = Modifier,
    text: String,
    state: ButtonState = ButtonState.Enabled,
    icon: ImageResource.Local? = null,
    iconColor: ButtonIconColor = ButtonIconColor.Default,
    onClick: () -> Unit
) {
    SecondaryButton(
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
private fun SecondaryButton(
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
        textColor = AppColors.backgroundSecondary,
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
private fun PreviewSecondaryButton() {
    SecondaryButton(
        text = "Button Text", state = ButtonState.Enabled, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSecondaryButtonDark() {
    PreviewSecondaryButton()
}

@Preview
@Composable
private fun PreviewSecondaryButtonSmall() {
    SecondarySmallButton(
        text = "Button Text", state = ButtonState.Enabled, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSecondaryButtonSmallDark() {
    PreviewSecondaryButtonSmall()
}

@Preview
@Composable
private fun PreviewSecondaryButtonDisabled() {
    SecondaryButton(
        text = "Button Text", state = ButtonState.Disabled, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSecondaryButtonDisabledDark() {
    PreviewSecondaryButtonDisabled()
}

@Preview
@Composable
private fun PreviewSecondaryButtonSmallDisabled() {
    SecondarySmallButton(
        text = "Button Text", state = ButtonState.Disabled, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSecondaryButtonSmallDisabledDark() {
    PreviewSecondaryButtonSmallDisabled()
}

@Preview
@Composable
private fun PreviewSecondaryButtonLoading() {
    SecondaryButton(
        text = "Button Text", state = ButtonState.Loading, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSecondaryButtonLoadingDark() {
    PreviewSecondaryButtonLoading()
}

@Preview
@Composable
private fun PreviewSecondaryButtonSmallLoading() {
    SecondarySmallButton(
        text = "Button Text", state = ButtonState.Loading, icon = Icons.Plus, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSecondaryButtonSmallLoadingDark() {
    PreviewSecondaryButtonSmallLoading()
}
