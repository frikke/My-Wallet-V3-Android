package piuk.blockchain.android.ui.multiapp.toolbar

interface CollapsingToolbarState {
    val consumed: Float
    var scrollTopLimitReached: Boolean
    var scrollOffset: Float
    var isInteractingWithPullToRefresh: Boolean
}