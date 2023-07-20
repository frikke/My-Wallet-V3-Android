package com.blockchain.componentlib.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppTheme

object Icons {
    object Filled
}

@Composable
fun ImageResource.Local.withBackground(
    backgroundColor: Color = AppTheme.colors.light,
    iconSize: Dp = size ?: AppTheme.dimensions.standardSpacing,
    backgroundSize: Dp = AppTheme.dimensions.xHugeSpacing,
    shape: Shape? = null
): ImageResource.LocalWithBackground = ImageResource.LocalWithBackground(
    id = id,
    iconColorFilter = colorFilter ?: ColorFilter.tint(AppTheme.colors.title),
    backgroundColor = backgroundColor,
    size = backgroundSize,
    iconSize = iconSize,
    alpha = 1f,
    shape = shape
)
