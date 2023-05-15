package com.blockchain.componentlib.tag

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark900
import com.blockchain.componentlib.theme.Red100
import com.blockchain.componentlib.theme.Red400
import com.blockchain.componentlib.theme.Red600

@Composable
fun ErrorTag(
    text: String,
    size: TagSize = TagSize.Primary,
    onClick: (() -> Unit)? = null
) {
    val defaultBackgroundColor = if (!isSystemInDarkTheme()) {
        Red100
    } else {
        Red400
    }

    val defaultTextColor = if (!isSystemInDarkTheme()) {
        Red600
    } else {
        Dark900
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
fun ErrorTag_Basic() {
    AppTheme {
        AppSurface {
            ErrorTag(text = "Default", onClick = null)
        }
    }
}

@Preview
@Composable
fun ErrorTag_Dark() {
    AppTheme(darkTheme = true) {
        AppSurface {
            ErrorTag(text = "Default", onClick = null)
        }
    }
}
