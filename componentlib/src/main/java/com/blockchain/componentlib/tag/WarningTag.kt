package com.blockchain.componentlib.tag

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppColors

@Composable
fun WarningTag(
    text: String,
    size: TagSize = TagSize.Primary,
    onClick: (() -> Unit)? = null
) {
    Tag(
        text = text,
        size = size,
        defaultBackgroundColor = AppColors.warningLight,
        defaultTextColor = AppColors.warning,
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
