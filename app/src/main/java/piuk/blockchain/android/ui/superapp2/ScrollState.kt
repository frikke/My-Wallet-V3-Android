package piuk.blockchain.android.ui.superapp2

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy

@Stable
class ScrollState(
    val heightRange: IntRange,
    scrollOffset: Float = 0F,
) {
    private val minHeight = heightRange.first
    private val maxHeight = heightRange.last
    private val rangeDifference = maxHeight - minHeight

    private var _scrollOffset by mutableStateOf(
        value = scrollOffset.coerceIn(0f, maxHeight.toFloat()),
        policy = structuralEqualityPolicy()
    )

    private var _consumed: Float = 0f
    val consumed = _consumed

    var scrollTopLimitReached: Boolean = true

    //    private val offset: Float
    //        get() = if (scrollOffset > rangeDifference) {
    //            -(scrollOffset - rangeDifference).coerceIn(0f, minHeight.toFloat())
    //        } else 0f

    var scrollOffset: Float
        get() = _scrollOffset
        set(value) {
            val oldOffset = _scrollOffset
            _scrollOffset = if (scrollTopLimitReached) {
                value.coerceIn(0f, maxHeight.toFloat())
            } else {
                value.coerceIn(rangeDifference.toFloat(), maxHeight.toFloat())
            }
            println("-----  scrollOffset _consumed ${_consumed}")
            println("-----  scrollOffset _scrollOffset ${_scrollOffset}")
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
                        scrollOffsetKey to it.scrollOffset,
                    )
                },
                restore = {
                    ScrollState(
                        heightRange = (it[minHeightKey] as Int)..(it[maxHeightKey] as Int),
                        scrollOffset = it[scrollOffsetKey] as Float,
                    )
                }
            )
        }
    }
}