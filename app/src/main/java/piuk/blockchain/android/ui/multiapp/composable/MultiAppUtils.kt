package piuk.blockchain.android.ui.multiapp.composable

import androidx.compose.foundation.lazy.LazyListState
import com.google.accompanist.swiperefresh.SwipeRefreshState

data class ListStateInfo(
    val firstVisibleItemIndex: Int,
    val firstVisibleItemScrollOffset: Int,
    val isSwipeInProgress: Boolean
)

fun extractStatesInfo(
    listState: LazyListState,
    swipeRefreshState: SwipeRefreshState?
): ListStateInfo {
    return ListStateInfo(
        firstVisibleItemIndex = listState.firstVisibleItemIndex,
        firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
        isSwipeInProgress = swipeRefreshState?.isSwipeInProgress ?: false
    )
}