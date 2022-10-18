package piuk.blockchain.android.ui.multiapp.toolbar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy

class EnterAlwaysCollapsedState(
    var topSectionHeight: Int,
    var bottomSectionHeight: Int,
    scrollOffset: Float = 0f
) : CollapsingToolbarState {

    override fun updateHeight(newTopSectionHeight: Int, newBottomSectionHeight: Int) {
        topSectionHeight = newTopSectionHeight
        bottomSectionHeight = newBottomSectionHeight
    }

    private val _halfCollapsedOffset get() = topSectionHeight
    override val halfCollapsedOffset: Float
        get() = _halfCollapsedOffset.toFloat()

    private val _fullCollapsedOffset get() = topSectionHeight + bottomSectionHeight
    override val fullCollapsedOffset: Float
        get() = _fullCollapsedOffset.toFloat()

    override val offsetValuesSet: Boolean
        get() = _halfCollapsedOffset > 0 && _fullCollapsedOffset > 0

    private var _consumed: Float = 0f
    override val consumed: Float
        get() = _consumed

    override var scrollTopLimitReached: Boolean = true

    override var isInteractingWithPullToRefresh: Boolean = false
    override var isAutoScrolling: Boolean = false

    private var _scrollOffset by mutableStateOf(
        value = scrollOffset.coerceIn(0f, _fullCollapsedOffset.toFloat()),
        policy = structuralEqualityPolicy()
    )
    override var scrollOffset: Float
        get() = _scrollOffset
        set(value) {
            val oldOffset = _scrollOffset

            _scrollState = when {
                value < oldOffset -> ScrollState.Up
                value > oldOffset -> ScrollState.Down
                else -> ScrollState.Idle
            }

            _scrollOffset = if (scrollTopLimitReached || isInteractingWithPullToRefresh || isAutoScrolling) {
                value.coerceIn(0f, _fullCollapsedOffset.toFloat())
            } else {
                value.coerceIn(_halfCollapsedOffset.toFloat(), _fullCollapsedOffset.toFloat())
            }

            _consumed = oldOffset - _scrollOffset
        }

    private var _scrollState: ScrollState = ScrollState.Idle
    override val scrollState: ScrollState
        get() = _scrollState

    companion object {
        val Saver = run {

            val topSectionHeight = "topSectionHeight"
            val bottomSectionHeight = "bottomSectionHeight"
            val scrollOffsetKey = "ScrollOffset"

            mapSaver(
                save = {
                    mapOf(
                        topSectionHeight to it.topSectionHeight,
                        bottomSectionHeight to it.bottomSectionHeight,
                        scrollOffsetKey to it.scrollOffset
                    )
                },
                restore = {
                    EnterAlwaysCollapsedState(
                        topSectionHeight = it[topSectionHeight] as Int,
                        bottomSectionHeight = it[bottomSectionHeight] as Int,
                        scrollOffset = it[scrollOffsetKey] as Float,
                    )
                }
            )
        }
    }
}
