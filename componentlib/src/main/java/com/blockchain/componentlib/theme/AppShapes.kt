package com.blockchain.componentlib.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

data class AppShapes(
    val small: RoundedCornerShape = RoundedCornerShape(4.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(8.dp),
    val large: RoundedCornerShape = RoundedCornerShape(16.dp),
    val veryLarge: RoundedCornerShape = RoundedCornerShape(24.dp),
    val extraLarge: RoundedCornerShape = RoundedCornerShape(100)
)

fun RoundedCornerShape.topOnly() = copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))

internal val LocalShapes = staticCompositionLocalOf { AppShapes() }
