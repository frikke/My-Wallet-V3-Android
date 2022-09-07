package piuk.blockchain.android.ui.superapp.dashboard.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Color
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

    var modeTrigger by remember {
        mutableStateOf(modes.first())
    }

    var selectedMode by remember {
        mutableStateOf(modes.first())
    }
    var previousSelectedMode by remember {
        mutableStateOf(modes.first())
    }

    val fullIndicatorWidth = 16F

    Row(modifier = modifier.fillMaxWidth()) {
        val indicatorWidth = remember { Animatable(fullIndicatorWidth) }
        LaunchedEffect(modeTrigger) {
            indicatorWidth.snapTo(fullIndicatorWidth - indicatorWidth.value)
            previousSelectedMode = selectedMode
            selectedMode = modeTrigger
            indicatorWidth.animateTo(
                targetValue = 16F,
                animationSpec = tween(
                    durationMillis = 400
                )
            )
        }

        val textAlpha = remember { Animatable(1F) }
        LaunchedEffect(modeTrigger) {
            textAlpha.snapTo(1F - textAlpha.value + 0.6F)
            textAlpha.animateTo(
                targetValue = 1F,
                animationSpec = tween(
                    durationMillis = 400
                )
            )
        }

        Spacer(modifier = Modifier.weight(1F))

        modes.forEachIndexed { index, mode ->
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickableNoEffect {
                        coroutineScopeAnimation.coroutineContext.cancelChildren()
                        modeTrigger = mode
                        onModeClicked(mode)
                    },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier,
                    style = AppTheme.typography.title3,
                    color = Color.White.copy(
                        alpha = if (selectedMode == mode) {
                            textAlpha.value
                        } else {
                            1F - textAlpha.value + 0.6F
                        }
                    ),
                    text = mode
                )

                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .width(
                            if (mode == selectedMode || mode == previousSelectedMode) {
                                if (selectedMode == mode) {
                                    indicatorWidth.value.dp
                                } else {
                                    (fullIndicatorWidth - indicatorWidth.value).dp
                                }
                            } else {
                                0.dp
                            }
                        )
                        .background(
                            color = Color.White.copy(
                                alpha = if (mode == selectedMode || mode == previousSelectedMode) {
                                    if (selectedMode == mode) {
                                        indicatorWidth.value / fullIndicatorWidth
                                    } else {
                                        1 - (indicatorWidth.value / fullIndicatorWidth)
                                    }
                                } else {
                                    0F
                                }

                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                )
            }

            if (modes.lastIndex != index) {
                Spacer(modifier = Modifier.size(32.dp))
            }
        }

        Spacer(modifier = Modifier.weight(1F))
    }
}