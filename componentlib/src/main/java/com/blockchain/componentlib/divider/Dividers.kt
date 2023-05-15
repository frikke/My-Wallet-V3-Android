package com.blockchain.componentlib.divider

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    dividerColor: Color = AppTheme.colors.light
) {
    Box(
        modifier = modifier
            .height(1.dp)
            .background(dividerColor)
    )
}

@Composable
fun VerticalDivider(
    modifier: Modifier = Modifier,
    dividerColor: Color = AppTheme.colors.light
) {
    Box(
        modifier = modifier
            .width(1.dp)
            .background(dividerColor)
    )
}
