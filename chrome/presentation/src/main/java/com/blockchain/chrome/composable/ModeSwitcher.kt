package com.blockchain.chrome.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.chrome.titleIcon
import com.blockchain.chrome.titleSuperApp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.walletmode.WalletMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.cancelChildren

@Composable
fun ModeSwitcher(
    modifier: Modifier = Modifier,
    modes: ImmutableList<WalletMode>,
    selectedMode: WalletMode,
    onModeClicked: (WalletMode) -> Unit,
    onModeLongClicked: (WalletMode) -> Unit
) {
    val coroutineScopeAnimation = rememberCoroutineScope()

    var currentMode: WalletMode? by remember { mutableStateOf(null) }
    var previousMode: WalletMode? by remember { mutableStateOf(null) }

    val fullIndicatorWidthPx = 16F

    val fullModeAlpha = 1F
    val minModeAlpha = 0.6F

    Row(modifier = modifier.fillMaxWidth()) {
        val animatableIndicatorWidthPx = remember { Animatable(fullIndicatorWidthPx) }
        LaunchedEffect(selectedMode) {
            animatableIndicatorWidthPx.snapTo(fullIndicatorWidthPx - animatableIndicatorWidthPx.value)
            previousMode = currentMode
            currentMode = selectedMode
            animatableIndicatorWidthPx.animateTo(
                targetValue = fullIndicatorWidthPx,
                animationSpec = tween(
                    durationMillis = ANIMATION_DURATION
                )
            )
        }

        val modeAlpha = remember { Animatable(fullModeAlpha) }
        LaunchedEffect(selectedMode) {
            modeAlpha.snapTo(fullModeAlpha - modeAlpha.value + minModeAlpha)
            modeAlpha.animateTo(
                targetValue = fullModeAlpha,
                animationSpec = tween(
                    durationMillis = ANIMATION_DURATION
                )
            )
        }

        Spacer(modifier = Modifier.weight(1F))

        modes.sortedBy { it.ordinal }.forEachIndexed { index, mode ->
            Column(
                modifier = Modifier
                    .clickableNoEffect(
                        onClick = {
                            if (currentMode != mode) {
                                coroutineScopeAnimation.coroutineContext.cancelChildren()
                                onModeClicked(mode)
                            }
                        },
                        onLongClick = {
                            onModeLongClicked(mode)
                        }
                    ),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
                Row(modifier = Modifier.wrapContentHeight()) {
                    val alpha: () -> Float = {
                        when (mode) {
                            currentMode -> modeAlpha.value
                            previousMode -> fullModeAlpha - modeAlpha.value + minModeAlpha
                            else -> minModeAlpha
                        }
                    }

                    mode.titleIcon()?.let { icon ->
                        Image(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(end = AppTheme.dimensions.tinySpacing)
                                .graphicsLayer {
                                    this.alpha = alpha()
                                },
                            imageResource = icon.withTint(AppTheme.colors.background)
                        )
                    }

                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .graphicsLayer {
                                this.alpha = alpha()
                            },
                        style = AppTheme.typography.title3,
                        color = AppTheme.colors.background,
                        text = stringResource(mode.titleSuperApp())
                    )
                }

                Box(
                    modifier = Modifier
                        .height(AppTheme.dimensions.smallestSpacing)
                        .width(fullIndicatorWidthPx.dp)
                        .graphicsLayer {
                            alpha = when (mode) {
                                currentMode -> {
                                    animatableIndicatorWidthPx.value / fullIndicatorWidthPx
                                }
                                previousMode -> {
                                    1 - (animatableIndicatorWidthPx.value / fullIndicatorWidthPx)
                                }
                                else -> {
                                    0F
                                }
                            }

                            scaleX = when (mode) {
                                currentMode -> {
                                    animatableIndicatorWidthPx.value / fullIndicatorWidthPx
                                }
                                previousMode -> {
                                    (fullIndicatorWidthPx - animatableIndicatorWidthPx.value) / fullIndicatorWidthPx
                                }
                                else -> {
                                    0F
                                }
                            }
                        }
                        .background(
                            color = AppTheme.colors.background,
                            shape = RoundedCornerShape(AppTheme.dimensions.standardSpacing)
                        )
                )

                Spacer(modifier = Modifier.size(12.dp))
            }

            if (modes.lastIndex != index) {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))
            }
        }

        Spacer(modifier = Modifier.weight(1F))
    }
}

@Preview
@Composable
fun PreviewModeSwitcher() {
    var selectedMode by remember { mutableStateOf(WalletMode.CUSTODIAL) }
    ModeSwitcher(
        modes = listOf(WalletMode.CUSTODIAL, WalletMode.NON_CUSTODIAL).toImmutableList(),
        selectedMode = selectedMode,
        onModeClicked = { selectedMode = it },
        onModeLongClicked = {}
    )
}
