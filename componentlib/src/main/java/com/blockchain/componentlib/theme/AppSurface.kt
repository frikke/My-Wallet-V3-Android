package com.blockchain.componentlib.theme

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AppSurface(color: Color = Color.Transparent, content: @Composable () -> Unit) {
    Surface(
        color = color,
        content = content
    )
}
