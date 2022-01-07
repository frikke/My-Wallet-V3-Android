package com.blockchain.componentlib.navigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.Blue600
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

@Composable
fun PulseLoading(
    modifier: Modifier = Modifier,
    durationMillis: Int = 2_000,
    maxPulseSize: Float = 104f,
    minPulseSize: Float = 32f
) {
    val easeInOut = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)
    val initialAlpha = 0.4f

    suspend fun animateCircleSize(update: (value: Float, velocity: Float) -> Unit) {
        animate(
            initialValue = minPulseSize,
            targetValue = maxPulseSize,
            animationSpec = TweenSpec(durationMillis = durationMillis, easing = easeInOut),
            block = update
        )
    }

    suspend fun animateCircleAlpha(update: (value: Float, velocity: Float) -> Unit) {
        animate(
            initialValue = initialAlpha,
            targetValue = 0f,
            animationSpec = TweenSpec(durationMillis = durationMillis, easing = easeInOut),
            block = update
        )
    }

    var firstCircleSize by remember { mutableStateOf(minPulseSize) }
    var firstCircleAlpha by remember { mutableStateOf(initialAlpha) }
    var secondCircleSize by remember { mutableStateOf(minPulseSize) }
    var secondCircleAlpha by remember { mutableStateOf(initialAlpha) }
    var thirdCircleSize by remember { mutableStateOf(minPulseSize) }
    var thirdCircleAlpha by remember { mutableStateOf(initialAlpha) }

    LaunchedEffect(Unit) {
        while (true) {
            val jobs = mutableListOf<Job>()

            jobs += launch {
                launch { animateCircleSize { value, _ -> firstCircleSize = value } }
                launch {
                    animateCircleAlpha { value, _ ->
                        firstCircleAlpha = value
                    }
                }
            }

            jobs += launch {
                delay(400)
                launch { animateCircleSize { value, _ -> secondCircleSize = value } }
                launch { animateCircleAlpha { value, _ -> secondCircleAlpha = value } }
            }

            jobs += launch {
                delay(800)
                launch { animateCircleSize { value, _ -> thirdCircleSize = value } }
                launch { animateCircleAlpha { value, _ -> thirdCircleAlpha = value } }
            }

            jobs.joinAll()

            firstCircleSize = minPulseSize
            firstCircleAlpha = 0.4f
            secondCircleSize = minPulseSize
            secondCircleAlpha = 0.4f
            thirdCircleSize = minPulseSize
            thirdCircleAlpha = 0.4f
        }
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(firstCircleSize.dp)
                .alpha(firstCircleAlpha)
                .background(color = Blue600, shape = CircleShape)
                .align(Alignment.Center)
        )
        Box(
            modifier = Modifier
                .size(secondCircleSize.dp)
                .alpha(secondCircleAlpha)
                .background(color = Blue600, shape = CircleShape)
                .align(Alignment.Center)
        )
        Box(
            modifier = Modifier
                .size(thirdCircleSize.dp)
                .alpha(thirdCircleAlpha)
                .background(color = Blue600, shape = CircleShape)
                .align(Alignment.Center)
        )
    }
}
