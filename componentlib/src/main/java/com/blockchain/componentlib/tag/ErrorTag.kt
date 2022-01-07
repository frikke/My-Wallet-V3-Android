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
import com.blockchain.componentlib.theme.Red100
import com.blockchain.componentlib.theme.Red400
import com.blockchain.componentlib.theme.Red600

@Composable
fun ErrorTag(text: String) {

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
fun ErrorTag_Basic() {
    AppTheme {
        AppSurface {
            ErrorTag(text = "Default")
        }
    }
}

@Preview
@Composable
fun ErrorTag_Dark() {
    AppTheme(darkTheme = true) {
        AppSurface {
            ErrorTag(text = "Default")
        }
    }
}
