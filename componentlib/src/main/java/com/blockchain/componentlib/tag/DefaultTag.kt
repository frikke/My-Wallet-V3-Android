package com.blockchain.componentlib.tag

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun DefaultTag(
    text: String,
    size: TagSize = TagSize.Primary,
    backgroundColor: Color = AppTheme.colors.backgroundSecondary,
    textColor: Color = AppTheme.colors.body,
    onClick: (() -> Unit)? = null
) {
    Tag(
        text = text,
        size = size,
        defaultBackgroundColor = backgroundColor,
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
