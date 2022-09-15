package piuk.blockchain.android.ui.multiapp.toolbar

interface CollapsingToolbarState {
    val fullCollapsedOffset: Float
    val halfCollapsedOffset: Float

    val offsetValuesSet: Boolean

    val consumed: Float
    var scrollTopLimitReached: Boolean
    var scrollOffset: Float
    var isInteractingWithPullToRefresh: Boolean
    var isAutoScrolling: Boolean

    fun updateHeight(newTopSectionHeight: Int, newBottomSectionHeight: Int)
}
