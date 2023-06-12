package com.blockchain.componentlib.tag

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.SnackbarDefaults.backgroundColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppTheme

private val bgColorLight = Color(0XFFF0F2F7)
private val bgColorDark = Color(0XFF3B3E46)
private val bgColor @Composable get() = if (isSystemInDarkTheme()) bgColorDark else bgColorLight

private val textColorLight = Color(0XFF121D33)
private val textColorDark = Color(0XFFFFFFFF)
private val textColor @Composable get() = if (isSystemInDarkTheme()) textColorDark else textColorLight

@Composable
fun DefaultTag(
    text: String,
    size: TagSize = TagSize.Primary,
    onClick: (() -> Unit)? = null
) {
    Tag(
        text = text,
        size = size,
        defaultBackgroundColor = bgColor,
        defaultTextColor = textColor,
        borders = true,
        onClick = onClick
    )
}

@Preview
@Composable
private fun DefaultTag_Basic() {
    DefaultTag(text = "Default", onClick = null)
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DefaultTag_Dark() {
    DefaultTag_Basic()
}

@Preview
@Composable
private fun DefaultTag_clickable() {
    DefaultTag(text = "Click me", onClick = {})
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DefaultTag_clickableDark() {
    DefaultTag_clickable()
}
