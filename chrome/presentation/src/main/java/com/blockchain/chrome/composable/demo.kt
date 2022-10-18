package com.blockchain.chrome.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.chrome.ListStateInfo
import com.blockchain.componentlib.chrome.extractStatesInfo
import com.blockchain.componentlib.swiperefresh.SwipeRefreshWithoutOverscroll
import com.blockchain.componentlib.theme.AppTheme
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * This screen shows the minimum setup needed to communicate the necessary info
 * to the main app switcher
 */
@Deprecated("only for reference when creating new screens")
@Composable
fun DemoScreen(
    modifier: Modifier = Modifier,
    tag: String,
    updateScrollInfo: (ListStateInfo) -> Unit,
    isPullToRefreshEnabled: Boolean,
    refreshStarted: () -> Unit,
    refreshComplete: () -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }

    /**
     * is mainly used to determine if the list is scrolled all the way to the top
     */
    val listState = rememberLazyListState()

    /**
     * is mainly used to determine the pull to refresh state
     */
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    val scope = rememberCoroutineScope()

    /**
     * send data to [MultiAppChromeScreen] to manage scroll and pull to refresh states correctly
     * if no pull to refresh is needed [swipeRefreshState] can be null
     */
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
        Column(
            modifier = modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
        ) {
            val aaaaa = mutableListOf<String>()
            (0..40).forEach { aaaaa.add("$tag $it") }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                items(
                    items = aaaaa,
                ) {
                    Text(
                        modifier = Modifier.padding(20.dp),
                        style = AppTheme.typography.title3,
                        color = Color.Black,
                        text = it
                    )
                }

                item {
                    Spacer(Modifier.size(70.dp))
                }
            }
        }
    }
}
