package com.blockchain.componentlib.tag

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue000
import com.blockchain.componentlib.theme.Blue400
import com.blockchain.componentlib.theme.Dark600

@Composable
fun InfoAltTag(text: String, size: TagSize = TagSize.Primary) {

    val defaultBackgroundColor = if (!isSystemInDarkTheme()) {
        Blue000
    } else {
        Dark600
    }

    val defaultTextColor = if (!isSystemInDarkTheme()) {
        Dark600
    } else {
        Blue400
    }

    Tag(
        text = text,
        size = size,
        defaultBackgroundColor = defaultBackgroundColor,
        defaultTextColor = defaultTextColor
    )
}

@Preview
@Composable
fun InfoAltTag_Basic() {
    AppTheme {
        AppSurface {
            InfoAltTag(text = "Default")
        }
    }
}

@Preview
@Composable
fun InfoAltTag_Dark() {
    AppTheme(darkTheme = true) {
        AppSurface {
            InfoAltTag(text = "Default")
        }
    }
}
