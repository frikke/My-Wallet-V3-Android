package com.blockchain.componentlib.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil.Coil
import coil.ImageLoader
import coil.compose.LocalImageLoader
import coil.decode.SvgDecoder

object AppTheme {

    val colors: SemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalColors.current

    val typography: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalTypography.current

    val dimensions: AppDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalDimensions.current

    val shapes: Shapes
        @Composable
        @ReadOnlyComposable
        get() = LocalShapes.current
}

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    typography: AppTypography = AppTheme.typography,
    dimensions: AppDimensions = AppTheme.dimensions,
    shapes: Shapes = AppTheme.shapes,
    content: @Composable () -> Unit
) {

    val imageLoader = ImageLoader.Builder(LocalContext.current)
        .componentRegistry { add(SvgDecoder(LocalContext.current)) }
        .crossfade(true)
        .build()

    val colors = if (darkTheme) getDarkColors() else getLightColors()
    val rememberedColors = remember { colors.copy() }.apply { updateColorsFrom(colors) }

    MaterialTheme(colors = debugColors(darkTheme)) {
        CompositionLocalProvider(
            LocalColors provides rememberedColors,
            LocalDimensions provides dimensions,
            LocalTypography provides typography,
            LocalShapes provides shapes,
            LocalRippleTheme provides AppThemeRippleProvider,
            LocalImageLoader provides imageLoader,
            content = content
        )
    }
}

/**
 * debugColors() is a utility function that visually exposes any composables that may implicitly use
 * MaterialTheme.colors, by showing the debugColor. This helps to enforce usage of AppTheme
 *
 */
fun debugColors(
    darkTheme: Boolean,
    debugColor: Color = Color.Magenta
) = Colors(
    primary = debugColor,
    primaryVariant = debugColor,
    secondary = debugColor,
    secondaryVariant = debugColor,
    background = debugColor,
    surface = debugColor,
    error = debugColor,
    onPrimary = debugColor,
    onSecondary = debugColor,
    onBackground = debugColor,
    onSurface = debugColor,
    onError = debugColor,
    isLight = !darkTheme
)