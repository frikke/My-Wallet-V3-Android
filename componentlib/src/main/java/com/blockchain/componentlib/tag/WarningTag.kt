package com.blockchain.componentlib.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun WarningTag(text: String) {
    Text(
        text = text,
        style = AppTheme.typography.caption2,
        color = AppTheme.colors.tagWarningText,
        modifier = Modifier
            .clip(AppTheme.shapes.small)
            .background(AppTheme.colors.tagWarningBackground)
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
