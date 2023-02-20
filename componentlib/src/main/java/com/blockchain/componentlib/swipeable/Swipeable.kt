package com.blockchain.componentlib.swipeable

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.blockchain.utils.CurrentTimeProvider
import kotlin.math.absoluteValue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

fun Modifier.swipeable(
    state: SwipeableState,
    onDrag: (hasReachedDismissThreshold: Boolean) -> Unit = {},
    onSwipe: (Direction) -> Unit,
    onSwipeComplete: (Direction) -> Unit,
    onSwipeCancel: () -> Unit = {}
) = pointerInput(Unit) {
    var flingStartTime = 0L
    var flingStartPosition = 0f

    suspend fun Direction.swipe() {
        onSwipe(this)
        state.swipe(this)
        onSwipeComplete(this)
    }

    coroutineScope {
        detectDragGestures(
            onDragStart = {
                flingStartTime = CurrentTimeProvider.currentTimeMillis()
                flingStartPosition = state.offsetProvider().targetValue.x
            },
            onDragCancel = {
                launch {
                    state.reset()
                    onSwipeCancel()
                }
            },
            onDrag = { change, dragAmount ->
                launch {
                    val original = state.offsetProvider().targetValue
                    val summed = original + dragAmount
                    val newValue = Offset.withX(summed.x.coerceIn(-state.maxWidth, state.maxWidth))

                    // consume the change so that we handle drag manually
                    change.consume()

                    state.drag(newValue.x)
                }

                onDrag(state.hasReachedDismissThreshold())
            },
            onDragEnd = {
                // calculate velocity
                val timeDelta = CurrentTimeProvider.currentTimeMillis() - flingStartTime
                val distance = state.offsetProvider().value.x - flingStartPosition
                val velocity = distance / timeDelta

                launch {
                    // if velocity is more than 0.5 let's handle it as a fling
                    if (velocity.absoluteValue > 0.5F) {
                        if (velocity > 0F) {
                            Direction.Right.swipe()
                        } else {
                            Direction.Left.swipe()
                        }
                    } else if (!state.hasReachedDismissThreshold()) {
                        state.reset()
                        onSwipeCancel()
                    } else {
                        if (state.offsetProvider().targetValue.x > 0) {
                            Direction.Right.swipe()
                        } else {
                            Direction.Left.swipe()
                        }
                    }
                }
            }
        )
    }
}.graphicsLayer {
    translationX = state.offsetProvider().value.x
    translationY = state.offsetProvider().value.y
}
