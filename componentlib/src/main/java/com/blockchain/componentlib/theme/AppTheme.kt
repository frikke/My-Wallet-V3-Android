package com.blockchain.componentlib.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.ImageLoader
import coil.compose.LocalImageLoader
import coil.decode.SvgDecoder
import com.blockchain.componentlib.BuildConfig
import com.google.accompanist.systemuicontroller.rememberSystemUiController

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

    val shapes: AppShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalShapes.current
}

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    typography: AppTypography = AppTheme.typography,
    dimensions: AppDimensions = AppTheme.dimensions,
    shapes: AppShapes = AppTheme.shapes,
    content: @Composable () -> Unit
) {

    val imageLoader = runCatching {
        ImageLoader.Builder(LocalContext.current)
            .componentRegistry { add(SvgDecoder(LocalContext.current)) }
            .crossfade(true)
            .build()
    }.getOrElse { throwable ->

        /**
         * Note the only reason we have this getOrElse block is for previews. For some reason, SvgDecoder fails to
         * be found/initialized when we are looking at jetpack compose previews in split view in Android Studio.
         * Inorder to allow for previews to work we have this try catch block. This is why we are throwing this error */
        if (BuildConfig.DEBUG.not()) {
            throw IllegalStateException("SVG Decoder failed, this should not happen in release", throwable)
        }
        /**
         * because if this code breaks in release we need to know about it, instead of silently failing as we are doing
         * here.
         * */
        ImageLoader.Builder(LocalContext.current)
            .componentRegistry { }
            .crossfade(true)
            .build()
    }

    val colors = if (darkTheme) getDarkColors() else getLightColors()
    val rememberedColors = remember { colors.copy() }.apply { updateColorsFrom(colors) }

    val navigationBackground = if (darkTheme) {
        Color.Black
    } else {
        Color.White
    }

    SystemColors(statusColor = colors.background, navigationColor = navigationBackground, isDarkTheme = darkTheme)

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

@Composable
fun SystemColors(statusColor: Color, navigationColor: Color, isDarkTheme: Boolean) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val systemUiController = rememberSystemUiController()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                systemUiController.setStatusBarColor(statusColor, !isDarkTheme)
                systemUiController.setNavigationBarColor(navigationColor, !isDarkTheme)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
