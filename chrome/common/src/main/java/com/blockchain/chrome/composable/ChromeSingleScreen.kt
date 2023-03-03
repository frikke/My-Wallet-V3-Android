package com.blockchain.chrome.composable

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.blockchain.chrome.backgroundColors
import com.blockchain.componentlib.navigation.ModeBackgroundColor
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
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
    isBottomSheet: Boolean,
    content: @Composable () -> Unit
) {
    val walletMode: WalletMode? by if (!isBottomSheet) {
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

    val statusBarHeight = if (!isBottomSheet) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    } else {
        0.dp
    }

    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    if ((!isBottomSheet == statusBarHeight > 0.dp) && navBarHeight > 0.dp) {
        // this container has the following format
        // -> Space for the status bar
        // -> main screen content
        // -> Space for the native android navigation
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(if (!isBottomSheet) 1F else 0.95F)
                .then(
                    if (walletMode == null || isBottomSheet) {
                        Modifier.background(AppTheme.colors.backgroundMuted)
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
            val (statusBar, navBar, content) = createRefs()

            Card(
                modifier = Modifier
                    .constrainAs(content) {
                        start.linkTo(parent.start)
                        top.linkTo(statusBar.bottom)
                        end.linkTo(parent.end)
                        bottom.linkTo(navBar.top)
                        height = Dimension.fillToConstraints
                    }
                    .fillMaxSize(),
                backgroundColor = Color(0XFFF1F2F7),
                shape = RoundedCornerShape(
                    topStart = AppTheme.dimensions.standardSpacing,
                    topEnd = AppTheme.dimensions.standardSpacing
                ),
                elevation = 0.dp
            ) {
                content()
            }

            // status bar
            Box(
                modifier = Modifier
                    .constrainAs(statusBar) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        end.linkTo(parent.end)
                    }
                    .fillMaxWidth()
                    .height(statusBarHeight)
            )

            // nav bar
            Box(
                modifier = Modifier
                    .constrainAs(navBar) {
                        start.linkTo(parent.start)
                        bottom.linkTo(parent.bottom)
                        end.linkTo(parent.end)
                    }
                    .fillMaxWidth()
                    .height(navBarHeight)
            )
        }
    }
}

@Composable
fun ChromeSingleScreen(
    backgroundColor: ModeBackgroundColor = ModeBackgroundColor.Current,
    content: @Composable () -> Unit
) {
    ChromeSingleScreen(
        backgroundColor = backgroundColor,
        isBottomSheet = false,
        content = content
    )
}

@Composable
fun ChromeBottomSheet(
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    BackHandler(onBack = onClose)
    ChromeSingleScreen(isBottomSheet = true, content = content)
}
