package com.blockchain.componentlib.chrome

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.blockchain.componentlib.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChromeListScreen(
    modifier: Modifier,
    isPullToRefreshEnabled: Boolean,
    refreshStarted: () -> Unit,
    refreshComplete: () -> Unit,
    updateStatesInfo: (ListStateInfo) -> Unit,
    content: @Composable (listState: LazyListState, shouldTriggerRefresh: Boolean) -> Unit
) {
    val listState = rememberLazyListState()

    ChromeScreen(
        modifier = modifier,
        state = listState,
        isPullToRefreshEnabled = isPullToRefreshEnabled,
        refreshStarted = refreshStarted,
        refreshComplete = refreshComplete,
        updateStatesInfo = updateStatesInfo,
        contentList = content
    )
}

@Composable
fun ChromeGridScreen(
    modifier: Modifier,
    isPullToRefreshEnabled: Boolean,
    refreshStarted: () -> Unit,
    refreshComplete: () -> Unit,
    updateStatesInfo: (ListStateInfo) -> Unit,
    content: @Composable (listState: LazyGridState, shouldTriggerRefresh: Boolean) -> Unit
) {
    val gridState = rememberLazyGridState()

    ChromeScreen(
        modifier = modifier,
        state = gridState,
        isPullToRefreshEnabled = isPullToRefreshEnabled,
        refreshStarted = refreshStarted,
        refreshComplete = refreshComplete,
        updateStatesInfo = updateStatesInfo,
        contentGrid = content
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ChromeScreen(
    modifier: Modifier,
    state: ScrollableState,
    isPullToRefreshEnabled: Boolean,
    refreshStarted: () -> Unit,
    refreshComplete: () -> Unit,
    updateStatesInfo: (ListStateInfo) -> Unit,
    contentList: @Composable ((listState: LazyListState, shouldTriggerRefresh: Boolean) -> Unit)? = null,
    contentGrid: @Composable ((listState: LazyGridState, shouldTriggerRefresh: Boolean) -> Unit)? = null
) {
    when (state) {
        is LazyListState -> {
            check(contentList != null)
        }
        is LazyGridState -> {
            check(contentGrid != null)
        }
        else -> {
            error("ScrollableState ${state::class.java} unsupported")
        }
    }

    val scope = rememberCoroutineScope()

    var isRefreshing by remember { mutableStateOf(false) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                refreshStarted()

                delay(2000)

                refreshComplete()
                isRefreshing = false
            }
        }
    )

    updateStatesInfo(extractStatesInfo(state, pullRefreshState))

    Box(
        Modifier.pullRefresh(
            state = pullRefreshState,
            enabled = isPullToRefreshEnabled
        )
    ) {
        Surface(
            modifier = modifier,
            color = Color.Transparent,
            shape = RoundedCornerShape(
                topStart = AppTheme.dimensions.standardSpacing,
                topEnd = AppTheme.dimensions.standardSpacing
            )
        ) {
            when (state) {
                is LazyListState -> {
                    check(contentList != null)
                    contentList(state, isRefreshing)
                }
                is LazyGridState -> {
                    check(contentGrid != null)
                    contentGrid(state, isRefreshing)
                }
                else -> {
                    error("ScrollableState ${state::class.java} unsupported")
                }
            }
        }

        PullRefreshIndicator(isRefreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
    }
}

data class ListStateInfo(
    val isFirstItemVisible: Boolean,
    val isFirstVisibleItemOffsetZero: Boolean,
    val isSwipeInProgress: Boolean
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun extractStatesInfo(
    state: ScrollableState,
    pullRefreshState: PullRefreshState
): ListStateInfo {
    return when (state) {
        is LazyListState -> {
            extractStatesInfo(
                listState = state,
                pullRefreshState = pullRefreshState
            )
        }
        is LazyGridState -> {
            extractStatesInfo(
                lazyGridState = state,
                pullRefreshState = pullRefreshState
            )
        }
        else -> {
            error("ScrollableState ${state::class.java} unsupported")
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun extractStatesInfo(
    listState: LazyListState,
    pullRefreshState: PullRefreshState
): ListStateInfo {
    return extractStatesInfo(
        isFirstItemVisibleProvider = { listState.firstVisibleItemIndex == 0 },
        isFirstVisibleItemOffsetZeroProvider = { listState.firstVisibleItemScrollOffset == 0 },
        isPullRefreshInProgressProvider = { pullRefreshState.progress > 0 }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun extractStatesInfo(
    lazyGridState: LazyGridState,
    pullRefreshState: PullRefreshState
): ListStateInfo {
    return extractStatesInfo(
        isFirstItemVisibleProvider = { lazyGridState.firstVisibleItemIndex == 0 },
        isFirstVisibleItemOffsetZeroProvider = { lazyGridState.firstVisibleItemScrollOffset == 0 },
        isPullRefreshInProgressProvider = { pullRefreshState.progress > 0 }
    )
}

@Composable
private fun extractStatesInfo(
    isFirstItemVisibleProvider: () -> Boolean,
    isFirstVisibleItemOffsetZeroProvider: () -> Boolean,
    isPullRefreshInProgressProvider: () -> Boolean,
): ListStateInfo {
    val isFirstItemVisible by remember {
        derivedStateOf {
            isFirstItemVisibleProvider()
        }
    }
    val isFirstVisibleItemOffsetZero by remember {
        derivedStateOf {
            isFirstVisibleItemOffsetZeroProvider()
        }
    }
    val isSwipeInProgress by remember {
        derivedStateOf {
            isPullRefreshInProgressProvider()
        }
    }

    return ListStateInfo(
        isFirstItemVisible = isFirstItemVisible,
        isFirstVisibleItemOffsetZero = isFirstVisibleItemOffsetZero,
        isSwipeInProgress = isSwipeInProgress
    )
}

@Composable
fun LazyListState.isScrollable(): Boolean {
    val value by remember {
        derivedStateOf {
            layoutInfo.visibleItemsInfo.size < layoutInfo.totalItemsCount
        }
    }
    return value
}
