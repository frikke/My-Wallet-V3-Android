package com.blockchain.componentlib.tag

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppColors

@Composable
fun ErrorTag(
    text: String,
    size: TagSize = TagSize.Primary,
    onClick: (() -> Unit)? = null
) {
    Tag(
        text = text,
        size = size,
        defaultBackgroundColor = AppColors.errorLight,
        defaultTextColor = AppColors.error,
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
