package piuk.blockchain.android.ui.multiapp.toolbar

interface CollapsingToolbarState {
    val fullHeight: Float
    val collapsedHeight: Float

    val consumed: Float
    var scrollTopLimitReached: Boolean
    var scrollOffset: Float
    var isInteractingWithPullToRefresh: Boolean

    fun updateHeight(heightRange2: IntRange)
}