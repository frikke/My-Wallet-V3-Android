package com.blockchain.componentlib.tag

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppColors

private val bgColorLight = Color(0XFFFFD9D6)
private val bgColorDark = Color(0XFFFF3344)
private val bgColor @Composable get() = if (isSystemInDarkTheme()) bgColorDark else bgColorLight

private val textColorLight = Color(0XFFCF1726)
private val textColorDark = Color(0XFF07080D)
private val textColor @Composable get() = if (isSystemInDarkTheme()) textColorDark else textColorLight

@Composable
fun ErrorTag(
    text: String,
    size: TagSize = TagSize.Primary,
    onClick: (() -> Unit)? = null
) {
    Tag(
        text = text,
        size = size,
        defaultBackgroundColor = bgColor,
        defaultTextColor = textColor,
        onClick = onClick
    )
}

@Preview
@Composable
fun ErrorTag_Basic() {
    ErrorTag(text = "Default", onClick = null)
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ErrorTag_BasicDark() {
    ErrorTag_Basic()
}

@Preview
@Composable
fun ErrorTag_clickable() {
    ErrorTag(text = "Click me", onClick = { })
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WarningTag_clickableDark() {
    ErrorTag_clickable()
}
