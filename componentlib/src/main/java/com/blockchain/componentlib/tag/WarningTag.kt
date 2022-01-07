package com.blockchain.componentlib.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark900
import com.blockchain.componentlib.theme.Orange100
import com.blockchain.componentlib.theme.Orange400
import com.blockchain.componentlib.theme.Orange600

@Composable
fun WarningTag(text: String) {

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

    Text(
        text = text,
        style = AppTheme.typography.caption2,
        color = defaultTextColor,
        modifier = Modifier
            .clip(AppTheme.shapes.small)
            .background(defaultBackgroundColor)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    )
}

@Preview
@Composable
fun WarningTag_Basic() {
    AppTheme {
        AppSurface {
            WarningTag(text = "Default")
        }
    }
}

@Preview
@Composable
fun WarningTag_Dark() {
    AppTheme(darkTheme = true) {
        AppSurface {
            WarningTag(text = "Default")
        }
    }
}
