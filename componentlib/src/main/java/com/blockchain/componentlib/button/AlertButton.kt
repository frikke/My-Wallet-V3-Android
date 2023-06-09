package com.blockchain.componentlib.button

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.common.Button
import com.blockchain.componentlib.button.common.ButtonIconColor
import com.blockchain.componentlib.button.common.ButtonStyle

private val bgColorLight = Color(0XFF121D33)
private val bgColorDark = Color(0XFF20242C)
private val bgColor @Composable get() = if (isSystemInDarkTheme()) bgColorDark else bgColorLight
private val textColor = Color(0XFFFFFFFF)

@Composable
fun AlertButton(
    modifier: Modifier = Modifier,
    text: String,
    state: ButtonState,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier,
        text = text,
        textColor = textColor,
        backgroundColor = bgColor,
        disabledBackgroundColor = bgColor,
        state = state,
        style = ButtonStyle.Default,
        icon = ImageResource.Local(R.drawable.ic_alert),
        iconColor = ButtonIconColor.Ignore,
        onClick = onClick
    )
}

// ------------ preview

@Preview
@Composable
private fun PreviewAlertButton() {
    AlertButton(
        text = "Button Text", state = ButtonState.Enabled, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAlertButtonDark() {
    PreviewAlertButton()
}

@Preview
@Composable
private fun PreviewAlertButtonDisabled() {
    AlertButton(
        text = "Button Text", state = ButtonState.Disabled, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAlertButtonDisabledDark() {
    PreviewAlertButtonDisabled()
}

@Preview
@Composable
private fun PreviewAlertButtonLoading() {
    AlertButton(
        text = "Button Text", state = ButtonState.Loading, onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAlertButtonLoadingDark() {
    PreviewAlertButtonLoading()
}
