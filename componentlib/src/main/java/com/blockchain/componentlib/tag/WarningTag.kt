package com.blockchain.componentlib.tag

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark900
import com.blockchain.componentlib.theme.Orange100
import com.blockchain.componentlib.theme.Orange400
import com.blockchain.componentlib.theme.Orange600

@Composable
fun WarningTag(
    text: String,
    size: TagSize = TagSize.Primary,
    onClick: (() -> Unit)? = null
) {
    val defaultBackgroundColor = if (!isSystemInDarkTheme()) {
        Orange100
    } else {
        Orange400
    }

    val defaultTextColor = if (!isSystemInDarkTheme()) {
        Orange600
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
fun WarningTag_Basic() {
    AppTheme {
        AppSurface {
            WarningTag(text = "Default", onClick = null)
        }
    }
}

@Preview
@Composable
fun WarningTag_Dark() {
    AppTheme(darkTheme = true) {
        AppSurface {
            WarningTag(text = "Default", onClick = null)
        }
    }
}
