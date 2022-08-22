package com.blockchain.componentlib.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AppDimensions(
    val xxxPaddingSmall: Dp = 1.dp,
    val xxPaddingSmall: Dp = 2.dp,
    val xPaddingSmall: Dp = 4.dp,
    val paddingSmall: Dp = 8.dp,
    val paddingMedium: Dp = 16.dp,
    val paddingLarge: Dp = 24.dp,
    val xPaddingLarge: Dp = 32.dp,
    val xxPaddingLarge: Dp = 40.dp,
    val xxxPaddingLarge: Dp = 48.dp,

    val xBorderRadiiSmall: Dp = 4.dp,
    val borderRadiiSmall: Dp = 8.dp,
    val borderRadiiMedium: Dp = 16.dp,
    val borderRadiiLarge: Dp = 100.dp,

    val xxElevationSmall: Dp = 2.dp,
)

internal val LocalDimensions = staticCompositionLocalOf { AppDimensions() }
