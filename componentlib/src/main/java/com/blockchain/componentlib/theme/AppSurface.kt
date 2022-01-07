package com.blockchain.componentlib.theme

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AppSurface(content: @Composable () -> Unit) {
    Surface(
        color = Color.Transparent,
        content = content
    )
}
