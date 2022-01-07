package com.blockchain.componentlib.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark600
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey900

@Composable
fun DefaultTag(text: String) {

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
