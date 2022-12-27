package com.blockchain.componentlib.tag

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark900
import com.blockchain.componentlib.theme.Green000
import com.blockchain.componentlib.theme.Green400
import com.blockchain.componentlib.theme.Green800

@Composable
fun SuccessTag(text: String, size: TagSize = TagSize.Primary, onClick: (() -> Unit)? = null) {

    val defaultBackgroundColor = if (!isSystemInDarkTheme()) {
        Green000
    } else {
        Green400
    }

    val defaultTextColor = if (!isSystemInDarkTheme()) {
        Green800
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
fun SuccessTag_Basic() {
    AppTheme {
        AppSurface {
            SuccessTag(text = "Default", onClick = null)
        }
    }
}

@Preview
@Composable
fun SuccessTag_Dark() {
    AppTheme(darkTheme = true) {
        AppSurface {
            SuccessTag(text = "Default", onClick = null)
        }
    }
}

@Preview
@Composable
fun SuccessTag_clickable() {
    AppTheme(darkTheme = true) {
        AppSurface {
            SuccessTag(text = "Click me", onClick = { })
        }
    }
}
