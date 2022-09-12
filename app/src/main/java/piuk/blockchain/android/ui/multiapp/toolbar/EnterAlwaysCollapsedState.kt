package piuk.blockchain.android.ui.multiapp.toolbar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy

class EnterAlwaysCollapsedState(
    var initialMinHeight: Int,
    var initialMaxHeight: Int,
    scrollOffset: Float = 0f
) : CollapsingToolbarState {

    override fun updateHeight(newMinHeight: Int, newMaxHeight: Int) {
        initialMinHeight = newMinHeight
        initialMaxHeight = newMaxHeight
    }

    private val minHeight get() = initialMinHeight
    override val collapsedHeight: Float
        get() = minHeight.toFloat()

    private val maxHeight get() = initialMaxHeight
    override val fullHeight: Float
        get() = maxHeight.toFloat()

    private var _consumed: Float = 0f
    override val consumed: Float
        get() = _consumed

    override var scrollTopLimitReached: Boolean = true

    override var isInteractingWithPullToRefresh: Boolean = false
    override var isAutoScrolling: Boolean = false

    private var _scrollOffset by mutableStateOf(
        value = scrollOffset.coerceIn(0f, maxHeight.toFloat()),
        policy = structuralEqualityPolicy()
    )
    override var scrollOffset: Float
        get() = _scrollOffset
        set(value) {
            val oldOffset = _scrollOffset
            _scrollOffset = if (scrollTopLimitReached || isInteractingWithPullToRefresh || isAutoScrolling) {
                value.coerceIn(0f, maxHeight.toFloat())
            } else {
                value.coerceIn(minHeight.toFloat(), maxHeight.toFloat())
            }

            _consumed = oldOffset - _scrollOffset
        }

    companion object {
        val Saver = run {

            val minHeightKey = "MinHeight"
            val maxHeightKey = "MaxHeight"
            val scrollOffsetKey = "ScrollOffset"

            mapSaver(
                save = {
                    mapOf(
                        minHeightKey to it.minHeight,
                        maxHeightKey to it.maxHeight,
                        scrollOffsetKey to it.scrollOffset
                    )
                },
                restore = {
                    EnterAlwaysCollapsedState(
                        initialMinHeight = it[minHeightKey] as Int,
                        initialMaxHeight = it[maxHeightKey] as Int,
                        scrollOffset = it[scrollOffsetKey] as Float,
                    )
                }
            )
        }
    }
}
