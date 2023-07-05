package com.blockchain.chrome.composable

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blockchain.chrome.backgroundColors
import com.blockchain.componentlib.navigation.ModeBackgroundColor
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.topOnly
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.componentlib.utils.conditional
import com.blockchain.koin.payloadScope
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import org.koin.androidx.compose.get

/**
 * Use this to wrap any screen to give it the right superapp ui
 *
 * Each new module/feature should have its own extension that defines composable with destinations,
 * wrap any of those screen with [ChromeSingleScreen]
 *
 * ```
 * fun NavGraphBuilder.homeGraph() {
 *     composable(navigationEvent = HomeDestination.CryptoAssets) {
 *         ChromeSingleScreen(
 *             content = {
 *                 CryptoAssets()
 *             }
 *         )
 *     }
 * }
 * ```
 */
@Composable
private fun ChromeSingleScreen(
    backgroundColor: ModeBackgroundColor = ModeBackgroundColor.Current,
    screenType: ScreenType,
    content: @Composable () -> Unit
) {
    val walletMode: WalletMode? by if (screenType is ScreenType.SingleScreen) {
        when (backgroundColor) {
            ModeBackgroundColor.Current -> {
                get<WalletModeService>(scope = payloadScope)
                    .walletMode.collectAsStateLifecycleAware(initial = null)
            }

            is ModeBackgroundColor.Override -> {
                remember { mutableStateOf(backgroundColor.walletMode) }
            }

            ModeBackgroundColor.None -> remember { mutableStateOf(null) }
        }
    } else {
        remember { mutableStateOf(null) }
    }

    val statusBarHeight = if (screenType is ScreenType.SingleScreen) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    } else {
        0.dp
    }

    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // this container has the following format
    // -> Space for the status bar
    // -> main screen content
    // -> Space for the native android navigation
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                when (screenType) {
                    is ScreenType.SingleScreen -> Modifier.fillMaxHeight()
                    is ScreenType.BottomSheet -> when (screenType.fillMaxHeight) {
                        true -> Modifier.fillMaxHeight(0.95F)
                        false -> Modifier.wrapContentHeight()
                    }
                }
            )
            .then(
                if (walletMode == null || screenType is ScreenType.BottomSheet) {
                    Modifier.background(Color.Unspecified)
                } else {
                    Modifier.background(
                        brush = Brush.horizontalGradient(
                            colors = walletMode!!
                                .backgroundColors()
                                .asList()
                        )
                    )
                }
            )
    ) {
        // status bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusBarHeight)
        )

        // content
        Surface(
            modifier = Modifier
                .conditional(screenType.shouldFillMaxHeight()) {
                    weight(1F)
                },
            color = Color.Unspecified,
            shape = AppTheme.shapes.veryLarge.topOnly()
        ) {
            content()
        }

        // nav bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(navBarHeight)
        )
    }
}

@Composable
fun ChromeSingleScreen(
    backgroundColor: ModeBackgroundColor = ModeBackgroundColor.Current,
    content: @Composable () -> Unit
) {
    ChromeSingleScreen(
        backgroundColor = backgroundColor,
        screenType = ScreenType.SingleScreen,
        content = content
    )
}

@Composable
fun ChromeBottomSheet(
    fillMaxHeight: Boolean = false,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    BackHandler(onBack = onClose)
    ChromeSingleScreen(
        screenType = ScreenType.BottomSheet(fillMaxHeight),
        content = content
    )
}

private sealed interface ScreenType {
    object SingleScreen : ScreenType
    data class BottomSheet(val fillMaxHeight: Boolean) : ScreenType
}

private fun ScreenType.shouldFillMaxHeight(): Boolean =
    when (this) {
        is ScreenType.SingleScreen -> true
        is ScreenType.BottomSheet -> fillMaxHeight
    }
