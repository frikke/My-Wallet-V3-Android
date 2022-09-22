package com.blockchain.componentlib.tag

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue200
import com.blockchain.componentlib.theme.Blue400
import com.blockchain.componentlib.theme.Dark600

@Composable
fun InfoAltTag(
    text: String,
    size: TagSize = TagSize.Primary,
    onClick: (() -> Unit)? = null
) {

    val defaultBackgroundColor = if (!isSystemInDarkTheme()) {
        Blue200
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
        defaultTextColor = defaultTextColor,
        onClick = onClick
    )
}

@Preview
@Composable
fun InfoAltTag_Basic() {
    AppTheme {
        AppSurface {
            InfoAltTag(text = "Default", onClick = null)
        }
    }
}

@Preview
@Composable
fun InfoAltTag_Dark() {
    AppTheme(darkTheme = true) {
        AppSurface {
            InfoAltTag(text = "Default", onClick = null)
        }
    }
}

@Preview
@Composable
fun InfoAltTag_clickable() {
    AppTheme(darkTheme = true) {
        AppSurface {
            InfoAltTag(text = "Click me", onClick = { })
        }
    }
}
