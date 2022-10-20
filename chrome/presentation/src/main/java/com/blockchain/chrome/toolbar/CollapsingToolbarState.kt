package com.blockchain.chrome.toolbar

interface CollapsingToolbarState {
    val fullCollapsedOffset: Float
    val halfCollapsedOffset: Float

    val offsetValuesSet: Boolean

    val consumed: Float
    var scrollTopLimitReached: Boolean
    var scrollOffset: Float
    val scrollState: ScrollState
    var isInteractingWithPullToRefresh: Boolean
    var isAutoScrolling: Boolean

    fun updateHeight(newTopSectionHeight: Int, newBottomSectionHeight: Int)
}

enum class ScrollState {
    Idle, Up, Down
}
