package piuk.blockchain.android.ui.superapp.dashboard.toolbar

import androidx.compose.runtime.Stable

@Stable
interface ToolbarState {
    val offset: Float
    val height: Float
    val progress: Float
    val consumed: Float
    var scrollTopLimitReached: Boolean
    var scrollOffset: Float
    var isInteractingWithPullToRefresh: Boolean
}