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
import com.blockchain.componentlib.theme.Blue000
import com.blockchain.componentlib.theme.Blue400
import com.blockchain.componentlib.theme.Dark600

@Composable
fun InfoAltTag(text: String) {

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
