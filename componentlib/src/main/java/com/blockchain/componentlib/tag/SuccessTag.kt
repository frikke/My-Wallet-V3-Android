package com.blockchain.componentlib.tag

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppColors

@Composable
fun SuccessTag(text: String, size: TagSize = TagSize.Primary, onClick: (() -> Unit)? = null) {
    Tag(
        text = text,
        size = size,
        defaultBackgroundColor = AppColors.successLight,
        defaultTextColor = AppColors.success,
        onClick = onClick
    )
}

@Preview
@Composable
private fun SuccessTag_Basic() {
    SuccessTag(text = "Default", onClick = null)
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SuccessTag_BasicDark() {
    SuccessTag_Basic()
}

@Preview
@Composable
fun SuccessTag_clickable() {
    SuccessTag(text = "Click me", onClick = { })
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SuccessTag_clickableDark() {
    SuccessTag_clickable()
}
