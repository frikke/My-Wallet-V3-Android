package com.blockchain.componentlib.tag

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark600
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey900

@Composable
fun DefaultTag(text: String, size: TagSize = TagSize.Primary) {

    val defaultBackgroundColor = if (!isSystemInDarkTheme()) {
        Grey000
    } else {
        Dark600
    }

    val defaultTextColor = if (!isSystemInDarkTheme()) {
        Grey900
    } else {
        Color.White
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
fun DefaultTag_Basic() {
    AppTheme {
        AppSurface {
            DefaultTag(text = "Default")
        }
    }
}

@Preview
@Composable
fun DefaultTag_Dark() {
    AppTheme(darkTheme = true) {
        AppSurface {
            DefaultTag(text = "Default")
        }
    }
}
