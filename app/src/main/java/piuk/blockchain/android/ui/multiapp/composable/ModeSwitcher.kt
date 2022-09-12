package piuk.blockchain.android.ui.multiapp.composable

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.clickableNoEffect
import kotlinx.coroutines.cancelChildren

@Composable
fun ModeSwitcher(
    modifier: Modifier = Modifier,
    modes: List<String>,
    onModeClicked: (String) -> Unit
) {

    val coroutineScopeAnimation = rememberCoroutineScope()

    var modeTrigger by remember { mutableStateOf(modes.first()) }

    var selectedMode by remember { mutableStateOf(modes.first()) }
    var previousSelectedMode by remember { mutableStateOf(modes.first()) }

    val fullIndicatorWidthPx = 16F

    val fullTextAlpha = 1F
    val minTextAlpha = 0.6F

    Row(modifier = modifier.fillMaxWidth()) {
        val animatableIndicatorWidthPx = remember { Animatable(fullIndicatorWidthPx) }
        LaunchedEffect(modeTrigger) {
            animatableIndicatorWidthPx.snapTo(fullIndicatorWidthPx - animatableIndicatorWidthPx.value)
            previousSelectedMode = selectedMode
            selectedMode = modeTrigger
            animatableIndicatorWidthPx.animateTo(
                targetValue = fullIndicatorWidthPx,
                animationSpec = tween(
                    durationMillis = ANIMATION_DURATION
                )
            )
        }

        val textAlpha = remember { Animatable(fullTextAlpha) }
        LaunchedEffect(modeTrigger) {
            textAlpha.snapTo(fullTextAlpha - textAlpha.value + minTextAlpha)
            textAlpha.animateTo(
                targetValue = fullTextAlpha,
                animationSpec = tween(
                    durationMillis = ANIMATION_DURATION
                )
            )
        }

        Spacer(modifier = Modifier.weight(1F))

        modes.forEachIndexed { index, mode ->
            Column(
                modifier = Modifier
                    .clickableNoEffect {
                        if (selectedMode != mode) {
                            coroutineScopeAnimation.coroutineContext.cancelChildren()
                            modeTrigger = mode
                            onModeClicked(mode)
                        }
                    },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.paddingSmall))

                Text(
                    modifier = Modifier,
                    style = AppTheme.typography.title3,
                    color = AppTheme.colors.background.copy(
                        alpha = if (selectedMode == mode) {
                            textAlpha.value
                        } else {
                            fullTextAlpha - textAlpha.value + minTextAlpha
                        }
                    ),
                    text = mode
                )

                Box(
                    modifier = Modifier
                        .height(AppTheme.dimensions.xPaddingSmall)
                        .width(
                            if (mode == selectedMode || mode == previousSelectedMode) {
                                if (selectedMode == mode) {
                                    animatableIndicatorWidthPx.value.dp
                                } else {
                                    (fullIndicatorWidthPx - animatableIndicatorWidthPx.value).dp
                                }
                            } else {
                                0.dp
                            }
                        )
                        .background(
                            color = AppTheme.colors.background.copy(
                                alpha = if (mode == selectedMode || mode == previousSelectedMode) {
                                    if (selectedMode == mode) {
                                        animatableIndicatorWidthPx.value / fullIndicatorWidthPx
                                    } else {
                                        1 - (animatableIndicatorWidthPx.value / fullIndicatorWidthPx)
                                    }
                                } else {
                                    0F
                                }

                            ),
                            shape = RoundedCornerShape(AppTheme.dimensions.paddingLarge)
                        )
                )

                Spacer(modifier = Modifier.size(12.dp))
            }

            if (modes.lastIndex != index) {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.xPaddingLarge))
            }
        }

        Spacer(modifier = Modifier.weight(1F))
    }
}

@Preview
@Composable
fun PreviewModeSwitcher() {
    ModeSwitcher(modes = listOf("Trading", "DeFi"), onModeClicked = {})
}