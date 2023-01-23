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

    fun balanceScrollAlpha(isFirstLaunch: Boolean): Float
    fun switcherScrollAlpha(): Float

    var isRefreshing: Boolean

    /**
     * if the screen is currently trying pull to refresh
     * i.e. is pulling and seeing the loading indicator
     * (refreshing is not triggered yet at this point, just the interaction swipe up and down)
     */
    var isPullToRefreshSwipeInProgress: Boolean

    /**
     * on first launch the total balance is revealed in the switcher area
     * and then animates back to switcher
     */
    var isBalanceRevealInProgress: Boolean

    fun updateHeight(newTopSectionHeight: Int, newBottomSectionHeight: Int)
}

enum class ScrollState {
    Idle, Up, Down
}
