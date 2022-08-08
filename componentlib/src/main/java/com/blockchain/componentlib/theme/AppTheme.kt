package com.blockchain.componentlib.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.flowWithLifecycle
import coil.ImageLoader
import coil.compose.LocalImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import com.blockchain.componentlib.BuildConfig
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.koin.java.KoinJavaComponent.get
import org.koin.mp.KoinPlatformTools

abstract class Theme {
    @get:Composable
    abstract val lightColors: SemanticColors

    @get:Composable
    abstract val darkColors: SemanticColors

    @get:Composable
    abstract val typography: AppTypography

    val colors: SemanticColors
        @Composable
        get() = if (isSystemInDarkTheme()) darkColors else lightColors

    @get:Composable
    abstract val dimensions: AppDimensions

    @get:Composable
    abstract val shapes: AppShapes
}

object DefaultAppTheme : Theme() {
    override val lightColors: SemanticColors
        @Composable
        @ReadOnlyComposable
        get() = defLightColors

    override val darkColors: SemanticColors
        @Composable
        @ReadOnlyComposable
        get() = defDarkColors

    override val typography: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalTypography.current

    override val dimensions: AppDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalDimensions.current

    override val shapes: AppShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalShapes.current
}

object AppTheme : Theme() {
    override val lightColors: SemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalLightColors.current

    override val darkColors: SemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalDarkColors.current

    override val typography: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalTypography.current

    override val dimensions: AppDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalDimensions.current

    override val shapes: AppShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalShapes.current
}

object FakeAppThemeProvider : AppThemeProvider {
    override val appTheme: Flow<Theme>
        get() = flow {
            AppTheme
        }
}

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeProvider: AppThemeProvider = defValue(),
    content: @Composable () -> Unit,
) {
    val imageLoader = runCatching {
        ImageLoader.Builder(LocalContext.current)
            .components {
                add(SvgDecoder.Factory())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
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
            .components { }
            .crossfade(true)
            .build()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(themeProvider.appTheme, lifecycleOwner) {
        themeProvider.appTheme.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val mTheme: Theme by stateFlowLifecycleAware.collectAsState(initial = DefaultAppTheme)

    val navigationBackground = if (darkTheme) {
        Color.Black
    } else {
        Color.White
    }

    val colorsLocalProvider = if (darkTheme)
        LocalDarkColors provides mTheme.colors.copy().apply { updateColorsFrom(mTheme.colors) }
    else
        LocalLightColors provides mTheme.colors.copy().apply { updateColorsFrom(mTheme.colors) }

    SystemColors(
        statusColor = mTheme.colors.background,
        navigationColor = navigationBackground,
        isDarkTheme = darkTheme
    )

    MaterialTheme(colors = debugColors(darkTheme)) {
        CompositionLocalProvider(
            colorsLocalProvider,
            LocalDimensions provides mTheme.dimensions,
            LocalTypography provides mTheme.typography,
            LocalShapes provides mTheme.shapes,
            LocalRippleTheme provides AppThemeRippleProvider,
            LocalImageLoader provides imageLoader,
            content = content
        )
    }
}

/**
 * We have to do this so Preview keeps working
 */
@Composable
fun defValue(): AppThemeProvider {
    return if (KoinPlatformTools.defaultContext().getOrNull() == null || LocalInspectionMode.current) {
        FakeAppThemeProvider
    } else {
        get(AppThemeProvider::class.java)
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
