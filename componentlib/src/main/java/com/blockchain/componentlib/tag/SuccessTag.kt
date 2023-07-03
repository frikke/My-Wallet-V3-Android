package com.blockchain.componentlib.tag

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ImageResource
import org.koin.android.BuildConfig

private val bgColorLight = Color(0XFFEDFFFA)
private val bgColorDark = Color(0XFF69ECCA)
private val bgColor @Composable get() = if (isSystemInDarkTheme()) bgColorDark else bgColorLight

private val textColorLight = Color(0XFF0C8868)
private val textColorDark = Color(0XFF07080D)
private val textColor @Composable get() = if (isSystemInDarkTheme()) textColorDark else textColorLight

@Composable
fun SuccessTag(
    text: String,
    startImageResource: ImageResource.Local? = null,
    size: TagSize = TagSize.Primary,
    onClick: (() -> Unit)? = null
) {
    Tag(
        text = text,
        size = size,
        startImageResource = startImageResource,
        backgroundColor = bgColor,
        textColor = textColor,
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
