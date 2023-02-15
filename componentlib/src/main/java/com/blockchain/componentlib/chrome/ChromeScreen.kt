package com.blockchain.componentlib.chrome

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.blockchain.componentlib.basic.ImageResource.None.shape
import com.blockchain.componentlib.swiperefresh.SwipeRefreshWithoutOverscroll
import com.blockchain.componentlib.theme.AppTheme
import com.google.accompanist.swiperefresh.SwipeRefreshState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChromeScreen(
    modifier: Modifier,
    isPullToRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    swipeRefreshState: SwipeRefreshState,
    content: @Composable (shouldTriggerRefresh: Boolean) -> Unit,
    refreshStarted: () -> Unit,
    refreshComplete: () -> Unit,
) {

    val scope = rememberCoroutineScope()

    SwipeRefreshWithoutOverscroll(
        state = swipeRefreshState,
        swipeEnabled = isPullToRefreshEnabled,
        onRefresh = {
            scope.launch {
                refreshStarted()
                delay(2000)
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
    swipeRefreshState: SwipeRefreshState
): ListStateInfo {
    return extractStatesInfo(
        isFirstItemVisibleProvider = { listState.firstVisibleItemIndex == 0 },
        isFirstVisibleItemOffsetZeroProvider = { listState.firstVisibleItemScrollOffset == 0 },
        isSwipeInProgressProvider = { swipeRefreshState.isSwipeInProgress }
    )
}

@Composable
fun extractStatesInfo(
    lazyGridState: LazyGridState,
    swipeRefreshState: SwipeRefreshState
): ListStateInfo {
    return extractStatesInfo(
        isFirstItemVisibleProvider = { lazyGridState.firstVisibleItemIndex == 0 },
        isFirstVisibleItemOffsetZeroProvider = { lazyGridState.firstVisibleItemScrollOffset == 0 },
        isSwipeInProgressProvider = { swipeRefreshState.isSwipeInProgress }
    )
}

@Composable
private fun extractStatesInfo(
    isFirstItemVisibleProvider: () -> Boolean,
    isFirstVisibleItemOffsetZeroProvider: () -> Boolean,
    isSwipeInProgressProvider: () -> Boolean,
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
            isSwipeInProgressProvider()
        }
    }

    return ListStateInfo(
        isFirstItemVisible = isFirstItemVisible,
        isFirstVisibleItemOffsetZero = isFirstVisibleItemOffsetZero,
        isSwipeInProgress = isSwipeInProgress
    )
}

@Composable
fun LazyListState.isScrollable() : Boolean{
    val value by remember {
        derivedStateOf {
            layoutInfo.visibleItemsInfo.size < layoutInfo.totalItemsCount
        }
    }
    return value
}