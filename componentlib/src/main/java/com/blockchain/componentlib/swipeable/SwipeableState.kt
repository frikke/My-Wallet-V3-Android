package com.blockchain.componentlib.swipeable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

const val ANIMATION_DURATION = 400

enum class Direction {
    Left, Right
}

@Composable
fun rememberSwipeableState(): SwipeableState {
    val screenWidth = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }
    return remember {
        SwipeableState(screenWidth)
    }
}

fun Offset.Companion.withX(x: Float): Offset {
    return Offset(x, 0f)
}

/**
 * @property thresholdVsMaxWidth value between 0..1, defines the threshold relative to screen width
 *
 * if [maxWidth] is 200, [thresholdVsMaxWidth] is 0.5 -> means you have to swipe the length of half a screen
 * for the dismiss to be active
 */
class SwipeableState(
    internal val maxWidth: Float,
    private val thresholdVsMaxWidth: Float = 0.25F
) {
    init {
        require(thresholdVsMaxWidth in 0F..1F)
    }

    private val _offset = Animatable(Offset.withX(0f), Offset.VectorConverter)
    val offsetProvider = { _offset }

    var swipedDirection: Direction? by mutableStateOf(null)
        private set

    internal suspend fun reset() {
        _offset.animateTo(Offset.withX(0f), tween(ANIMATION_DURATION))
    }

    suspend fun swipe(
        direction: Direction,
        animationSpec: AnimationSpec<Offset> = tween(ANIMATION_DURATION)
    ) {
        val endX = maxWidth * 1.5f
        when (direction) {
            Direction.Left -> _offset.animateTo(Offset.withX(x = -endX), animationSpec)
            Direction.Right -> _offset.animateTo(Offset.withX(x = endX), animationSpec)
        }
        this.swipedDirection = direction
    }

    internal suspend fun drag(x: Float) {
        _offset.animateTo(Offset.withX(x))
    }

    fun hasReachedDismissThreshold(): Boolean {
        val coercedOffset = Offset(
            x = _offset.targetValue.x.coerceIn(-maxWidth, maxWidth),
            y = 0f
        )
        return abs(coercedOffset.x) > maxWidth * thresholdVsMaxWidth
    }
}
