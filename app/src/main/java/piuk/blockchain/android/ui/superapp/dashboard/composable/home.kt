package piuk.blockchain.android.ui.superapp.dashboard.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppTheme
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import piuk.blockchain.android.R

@Composable
fun HomeScreen(modifier: Modifier = Modifier, indexedChanged: (Pair<Int, Int>) -> Unit, enableRefresh: Boolean) {
    val listState = rememberLazyListState()

    indexedChanged(Pair(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset))

    var isRefreshing by remember {
        mutableStateOf(false)
    }
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
    val scope = rememberCoroutineScope()
    SwipeRefreshWithoutOverscroll(
        state = swipeRefreshState,
        swipeEnabled = enableRefresh,
        onRefresh = {
            isRefreshing = true
            scope.launch {
                delay(2000)
                isRefreshing = false
            }
        },
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                //            .background(Color.Yellow)
                .wrapContentSize(Alignment.Center)
        ) {
            val aaaaa = mutableListOf<String>()
            (0..40).forEach { aaaaa.add("abc $it") }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    //                            .graphicsLayer { translationY = toolbarState.height + toolbarState.offset  }
                    //                            .pointerInput(Unit) {
                    //                                detectTapGestures(
                    //                                    onPress = {
                    //                                        scope.coroutineContext.cancelChildren()
                    //                                        coroutineScopeAnim.coroutineContext.cancelChildren()
                    //                                        animate = false
                    //                                    }
                    //                                )
                    //                            }
                    .background(Color(0XFFF1F2F7), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
            ) {
                items(
                    items = aaaaa,
                ) {
                    Text(
                        modifier = Modifier.padding(dimensionResource(R.dimen.very_small_margin)),
                        style = AppTheme.typography.title3,
                        color = Color.Black,
                        text = it
                    )
                }

                item {
                    Spacer(Modifier.size(dimensionResource(R.dimen.epic_margin)))
                }
            }
        }
    }
}

@Composable
fun CardScreen(modifier: Modifier = Modifier, indexedChanged: (Pair<Int, Int>) -> Unit, enableRefresh: Boolean) {
    val listState = rememberLazyListState()

    println("----- listState.firstVisibleItemIndex CardScreen ${listState.firstVisibleItemIndex}")

    indexedChanged(Pair(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset))

    var isRefreshing by remember {
        mutableStateOf(false)
    }
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
    val scope = rememberCoroutineScope()

    SwipeRefreshWithoutOverscroll(
        state = swipeRefreshState,
        swipeEnabled = enableRefresh,
        onRefresh = {
            isRefreshing = true
            scope.launch {
                delay(2000)
                isRefreshing = false
            }
        },
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                //            .background(Color.Blue)
                .wrapContentSize(Alignment.Center)
        ) {
            val aaaaa = mutableListOf<String>()
            (0..40).forEach { aaaaa.add("abcder $it") }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    //                            .graphicsLayer { translationY = toolbarState.height + toolbarState.offset  }
                    //                            .pointerInput(Unit) {
                    //                                detectTapGestures(
                    //                                    onPress = {
                    //                                        scope.coroutineContext.cancelChildren()
                    //                                        coroutineScopeAnim.coroutineContext.cancelChildren()
                    //                                        animate = false
                    //                                    }
                    //                                )
                    //                            }
                    .background(Color(0XFFF1F2F7), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
            ) {
                items(
                    items = aaaaa,
                ) {
                    Text(
                        modifier = Modifier.padding(dimensionResource(R.dimen.very_small_margin)),
                        style = AppTheme.typography.title3,
                        color = Color.Black,
                        text = it
                    )
                }

                item {
                    Spacer(Modifier.size(dimensionResource(R.dimen.epic_margin)))
                }
            }
        }
    }
}