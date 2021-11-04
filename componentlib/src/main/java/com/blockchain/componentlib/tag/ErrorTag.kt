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
fun ErrorTag(text: String) {
    Text(
        text = text,
        style = AppTheme.typography.caption2,
        color = AppTheme.colors.tagErrorText,
        modifier = Modifier
            .clip(AppTheme.shapes.small)
            .background(AppTheme.colors.tagErrorBackground)
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
