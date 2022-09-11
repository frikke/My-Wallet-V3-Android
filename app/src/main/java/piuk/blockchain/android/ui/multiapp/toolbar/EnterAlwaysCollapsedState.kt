package piuk.blockchain.android.ui.multiapp.toolbar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy

class EnterAlwaysCollapsedState(
    var heightRange: IntRange,
    scrollOffset: Float = 0f
) : CollapsingToolbarState {

//    init {
//        require(heightRange.first >= 0 && heightRange.last >= heightRange.first) {
//            "first range is lower than last"
//        }
//    }

   override fun updateHeight(heightRange2: IntRange){
        heightRange = heightRange2
    }

    private val minHeight get() = heightRange.first
    override val collapsedHeight: Float
        get() = minHeight.toFloat()

    private val maxHeight get() = heightRange.last
    override val fullHeight: Float
        get() = maxHeight.toFloat()

    private val rangeDifference get() = maxHeight - minHeight

    private var _consumed: Float = 0f
    override val consumed: Float
        get() = _consumed

    override var scrollTopLimitReached: Boolean = true

    override var isInteractingWithPullToRefresh: Boolean = false

    private var _scrollOffset by mutableStateOf(
        value = scrollOffset.coerceIn(0f, maxHeight.toFloat()),
        policy = structuralEqualityPolicy()
    )
    override var scrollOffset: Float
        get() = _scrollOffset
        set(value) {
            val oldOffset = _scrollOffset
            _scrollOffset = if (scrollTopLimitReached || isInteractingWithPullToRefresh) {
                value.coerceIn(0f, maxHeight.toFloat())
            } else {
                value.coerceIn(rangeDifference.toFloat(), maxHeight.toFloat())
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
                        heightRange = (it[minHeightKey] as Int)..(it[maxHeightKey] as Int),
                        scrollOffset = it[scrollOffsetKey] as Float,
                    )
                }
            )
        }
    }
}