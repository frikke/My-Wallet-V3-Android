package piuk.blockchain.android.ui.superapp.dashboard.composable

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.END_DEFI
import com.blockchain.componentlib.theme.END_TRADING
import com.blockchain.componentlib.theme.START_DEFI
import com.blockchain.componentlib.theme.START_TRADING
import kotlinx.coroutines.cancelChildren
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.multiapp.toolbar.EnterAlwaysCollapsedState
import piuk.blockchain.android.ui.multiapp.toolbar.CollapsingToolbarState

private val MinToolbarHeight = 54.dp
private val MaxToolbarHeight = 108.dp

@Composable
private fun rememberToolbarState(toolbarHeightRange: IntRange): CollapsingToolbarState {
    return rememberSaveable(saver = EnterAlwaysCollapsedState.Saver) {
        EnterAlwaysCollapsedState(toolbarHeightRange)
    }
}

@Composable
fun SuperAppDashboard() {
    val heightOnTop = 0F
    val heightUntilTotalBalance = LocalDensity.current.run { MinToolbarHeight.toPx() }
    val collapsedBalanceOffset = LocalDensity.current.run { MinToolbarHeight.toPx() }
    val allCollapsedOffset = LocalDensity.current.run { MaxToolbarHeight.toPx() }
    val sbHeight = LocalDensity.current.run { WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx() }

    val toolbarHeightRange = with(LocalDensity.current) {
        MinToolbarHeight.roundToPx()..MaxToolbarHeight.roundToPx()
    }
    val toolbarState = rememberToolbarState(toolbarHeightRange)
    val listState = rememberLazyListState()

    val coroutineScopeAnim = rememberCoroutineScope()

    val scope = rememberCoroutineScope()

    val offsetY = remember { Animatable(0f) }
    var animate by remember {
        mutableStateOf(false)
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                toolbarState.scrollTopLimitReached =
                    listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                toolbarState.scrollOffset = toolbarState.scrollOffset - available.y
                return Offset(0f, toolbarState.consumed)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
//                // hide total balance if the offset is 1/2 below its height
//                if (toolbarState.scrollOffset > heightOnTop &&
//                    toolbarState.scrollOffset < collapsedBalanceOffset
//                ) {
//                    coroutineScopeAnim.launch {
//                        animate = true
//                        offsetY.snapTo(toolbarState.scrollOffset)
//                        offsetY.animateTo(
//                            targetValue = if (toolbarState.scrollOffset > heightUntilTotalBalance / 2) {
//                                collapsedBalanceOffset
//                            } else {
//                                heightOnTop
//                            },
//                            animationSpec = tween(
//                                durationMillis = 400
//                            )
//                        )
//                    }
//                }
//                // if switcher is scrolled but still visible, snap to the top of it
//                if (toolbarState.scrollOffset > heightUntilTotalBalance &&
//                    toolbarState.scrollOffset < allCollapsedOffset
//                ) {
//                    coroutineScopeAnim.launch {
//                        animate = true
//                        offsetY.snapTo(toolbarState.scrollOffset)
//                        offsetY.animateTo(
//                            targetValue = heightUntilTotalBalance,
//                            animationSpec = tween(
//                                durationMillis = 400
//                            )
//                        )
//                    }
//                }

//                if (available.y > 0) {
//                    scope.launch {
//                        animateDecay(
//                            initialValue = toolbarState.height + toolbarState.offset,
//                            initialVelocity = available.y,
//                            animationSpec = FloatExponentialDecaySpec()
//                        ) { value, velocity ->
//                            toolbarState.scrollTopLimitReached =
//                                listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
//                            toolbarState.scrollOffset =
//                                toolbarState.scrollOffset - (value - (toolbarState.height + toolbarState.offset))
//                            // when flinging, stop scrolling as soon as the top is reached (switcher would already be visible)
//                            // we don't want to show total balance until user explicitly scroll again on top
//                            if (toolbarState.scrollTopLimitReached) scope.coroutineContext.cancelChildren()
//
//                        }
//                    }
//                }

                return super.onPostFling(consumed, available)
            }
        }
    }

    //////

    var trading by remember {
        mutableStateOf(true)
    }

    var shouldFlash by remember { mutableStateOf(false) }

    val animateDown by animateIntAsState(
        targetValue = if (shouldFlash) 300 else 0,
        finishedListener = {
            if (shouldFlash) {
                trading = trading.not()
                shouldFlash = false
            }
        },
        animationSpec = tween(
            durationMillis = 200,
            delayMillis = 0
        )
    )

    val aaaaa = mutableListOf<String>()
    (0..40).forEach { aaaaa.add("abc $it") }

    var switch by remember { mutableStateOf(true) }
    val startColor by animateColorAsState(
        targetValue = if (switch) START_TRADING else START_DEFI,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = 0
        )
    )

    val endColor by animateColorAsState(
        targetValue = if (switch) END_TRADING else END_DEFI,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = 0
        )
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        startColor,
                        endColor
                    )
                )
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
//                    .graphicsLayer { translationY = toolbarState.height + toolbarState.offset  }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                scope.coroutineContext.cancelChildren()
                                coroutineScopeAnim.coroutineContext.cancelChildren()
                                animate = false
                            }
                        )
                    }
                    .background(Color(0XFFF1F2F7), RoundedCornerShape(20.dp)),
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

            if (animate) {
                toolbarState.scrollOffset = offsetY.value
                if (toolbarState.scrollOffset == allCollapsedOffset) {
                    animate = false
                }
            }

//            CollapsingToolbar(
//                progress = toolbarState.progress,
//                onPrivacyTipButtonClicked = {
//                    coroutineScopeAnim.launch {
//                        animate = true
//                        offsetY.snapTo(toolbarState.scrollOffset)
//                        offsetY.animateTo(
//                            targetValue = allCollapsedOffset,
//                            animationSpec = tween(
//                                durationMillis = 400
//                            )
//                        )
//                    }
//                },
//                onTradingClicked = {
//                    shouldFlash = true
//                    switch = true
//                },
//                onDefiClicked = {
//                    shouldFlash = true
//                    switch = false
//                },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(with(LocalDensity.current) { toolbarState.height.toDp() })
//                    .graphicsLayer { translationY = toolbarState.offset }
//            )

            // bottom navigation
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset {
                        IntOffset(
                            x = 0,
                            y = animateDown
                        )
                    }
            ) {
                Card(
                    elevation = 15.dp,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Row(modifier = Modifier.padding(vertical = 8.dp)) {
                        Spacer(Modifier.size(dimensionResource(R.dimen.large_margin)))

                        com.blockchain.componentlib.basic.Image(
                            imageResource = ImageResource.Local(R.drawable.ic_tab_home_demo)
                        )

                        Spacer(modifier = Modifier.size(24.dp))

                        com.blockchain.componentlib.basic.Image(
                            imageResource = ImageResource.Local(R.drawable.ic_tab_itemtrade)
                        )

                        Spacer(modifier = Modifier.size(24.dp))

                        com.blockchain.componentlib.basic.Image(
                            imageResource = ImageResource.Local(R.drawable.ic_tab_item_card)
                        )

                        Spacer(Modifier.size(dimensionResource(R.dimen.large_margin)))
                    }
                }

                Spacer(modifier = Modifier.size(32.dp))

            }
        }
    }
}

@Preview
@Composable
fun PreviewMain() {
    SuperAppDashboard()
}