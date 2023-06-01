package com.blockchain.componentlib.tag

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue200
import com.blockchain.componentlib.theme.Dark600

@Composable
fun InfoAltTag(
    text: String,
    size: TagSize = TagSize.Primary,
    onClick: (() -> Unit)? = null
) {
    Tag(
        text = text,
        size = size,
        defaultBackgroundColor = AppColors.primaryLight,
        defaultTextColor = AppColors.primary,
        onClick = onClick
    )
}

@Preview
@Composable
private fun InfoAltTag_Basic() {
    InfoAltTag(text = "Default", onClick = null)
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InfoAltTag_BasicDark() {
    InfoAltTag_Basic()
}

@Preview
@Composable
fun InfoAltTag_clickable() {
    InfoAltTag(text = "Click me", onClick = { })
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InfoAltTag_clickableDark() {
    InfoAltTag_clickable()
}
