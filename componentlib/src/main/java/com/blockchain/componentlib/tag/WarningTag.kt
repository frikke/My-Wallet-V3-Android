package com.blockchain.componentlib.tag

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

private val bgColorLight = Color(0XFFFFECD6)
private val bgColorDark = Color(0XFFFFA133)
private val bgColor @Composable get() = if (isSystemInDarkTheme()) bgColorDark else bgColorLight

private val textColorLight = Color(0XFFD46A00)
private val textColorDark = Color(0XFF07080D)
private val textColor @Composable get() = if (isSystemInDarkTheme()) textColorDark else textColorLight

@Composable
fun WarningTag(
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
fun WarningTag_Basic() {
    WarningTag(text = "Default", onClick = null)
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WarningTag_BasicDark() {
    WarningTag_Basic()
}

@Preview
@Composable
fun WarningTag_clickable() {
    WarningTag(text = "Click me", onClick = { })
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WarningTag_clickableDark() {
    WarningTag_clickable()
}
