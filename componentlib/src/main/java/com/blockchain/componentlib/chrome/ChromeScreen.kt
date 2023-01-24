package com.blockchain.componentlib.chrome

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.blockchain.componentlib.basic.ImageResource.None.shape
import com.blockchain.componentlib.swiperefresh.SwipeRefreshWithoutOverscroll
import com.blockchain.componentlib.theme.AppTheme
import com.google.accompanist.swiperefresh.SwipeRefreshState
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChromeScreen(
    modifier: Modifier,
    updateScrollInfo: (ListStateInfo) -> Unit,
    isPullToRefreshEnabled: Boolean,
    listState: LazyListState,
    content: @Composable (shouldTriggerRefresh: Boolean) -> Unit,
    refreshStarted: () -> Unit,
    refreshComplete: () -> Unit,
) {

    var isRefreshing by remember { mutableStateOf(false) }

    /**
     * is mainly used to determine the pull to refresh state
     */
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    val scope = rememberCoroutineScope()
    updateScrollInfo(
        extractStatesInfo(listState, swipeRefreshState)
    )

    SwipeRefreshWithoutOverscroll(
        state = swipeRefreshState,
        swipeEnabled = isPullToRefreshEnabled,
        onRefresh = {
            isRefreshing = true
            scope.launch {
                refreshStarted()
                delay(2000)
                isRefreshing = false
                refreshComplete()
            }
        },
    ) {
        Surface(
            modifier = modifier,
            color = Color.Transparent,
            shape = RoundedCornerShape(
                topStart = AppTheme.dimensions.standardSpacing,
                topEnd = AppTheme.dimensions.standardSpacing
            )
        ) {
            content(isRefreshing)
        }
    }
}

data class ListStateInfo(
    val isFirstItemVisible: Boolean,
    val isFirstVisibleItemOffsetZero: Boolean,
    val isSwipeInProgress: Boolean
)

@Composable
fun extractStatesInfo(
    listState: LazyListState,
    swipeRefreshState: SwipeRefreshState?
): ListStateInfo {
    val isFirstItemVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0
        }
    }
    val isFirstVisibleItemOffsetZero by remember {
        derivedStateOf {
            listState.firstVisibleItemScrollOffset == 0
        }
    }
    val isSwipeInProgress by remember {
        derivedStateOf {
            swipeRefreshState?.isSwipeInProgress ?: false
        }
    }

    return ListStateInfo(
        isFirstItemVisible = isFirstItemVisible,
        isFirstVisibleItemOffsetZero = isFirstVisibleItemOffsetZero,
        isSwipeInProgress = isSwipeInProgress
    )
}

val LazyListState.isScrollable: Boolean
    get() = layoutInfo.visibleItemsInfo.size < layoutInfo.totalItemsCount
