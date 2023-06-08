package com.blockchain.componentlib.button

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.common.Button
import com.blockchain.componentlib.button.common.ButtonStyle
import com.blockchain.componentlib.theme.AppTheme

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
        state = state,
        backgroundColor = bgColor,
        disabledBackgroundColor = bgColor,
        contentPadding = ButtonStyle.Default.contentPadding,
        onClick = onClick,
        buttonContent = {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(ImageResource.Local(R.drawable.ic_alert))
                Spacer(Modifier.width(AppTheme.dimensions.tinySpacing))
                Text(
                    text = text,
                    color = textColor,
                    style = ButtonStyle.Default.textStyle,
                    textAlign = TextAlign.Center
                )
            }
        }
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
