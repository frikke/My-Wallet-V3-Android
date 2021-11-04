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
fun InfoAltTag(text: String) {
    Text(
        text = text,
        style = AppTheme.typography.caption2,
        color = AppTheme.colors.tagInfoAltText,
        modifier = Modifier
            .clip(AppTheme.shapes.small)
            .background(AppTheme.colors.tagInfoAltBackground)
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
