package piuk.blockchain.android.ui.superapp2

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.compose.rememberNavController
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.END_DEFI
import com.blockchain.componentlib.theme.END_TRADING
import com.blockchain.componentlib.theme.START_DEFI
import com.blockchain.componentlib.theme.START_TRADING
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.multiapp.composable.ModeSwitcher
import piuk.blockchain.android.ui.multiapp.composable.TotalBalance
import piuk.blockchain.android.ui.multiapp.toolbar.CollapsingToolbarState
import piuk.blockchain.android.ui.multiapp.toolbar.EnterAlwaysCollapsedState
import piuk.blockchain.android.ui.superapp.dashboard.composable.BottomNavigationC
import piuk.blockchain.android.ui.superapp.dashboard.composable.NavigationGraph

@Composable
private fun rememberToolbarState(min: Int, max: Int): CollapsingToolbarState {
    return rememberSaveable(saver = EnterAlwaysCollapsedState.Saver) {
        EnterAlwaysCollapsedState(min, max)
    }
}

//lateinit var toolbarState: EnterAlwaysCollapsedState

@Composable
fun MultiAppDashboard() {
    //    var heightIs by remember {
    //        mutableStateOf(0)
    //    }

    var balanceSectionHeight by remember {
        mutableStateOf(0)
    }
    var tabsSectionHeight by remember {
        mutableStateOf(0)
    }

    var toolbarState = rememberToolbarState(0, 1)

    //    if (heightIs > 0) {
    //        println("-----  heightIs ${heightIs}")
    //        toolbarState = rememberToolbarState((heightIs / 2)..(heightIs))
    //    }
    var firstVisibleItemIndex by remember {
        mutableStateOf(0)
    }
    var firstVisibleItemScrollOffset by remember {
        mutableStateOf(0)
    }
    var isSwipeInProgress by remember {
        mutableStateOf(false)
    }

    // snaps
    val coroutineScopeAnim = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    var animate by remember {
        mutableStateOf(false)
    }

    if (animate) {
        toolbarState.scrollOffset = offsetY.value
        if (toolbarState.scrollOffset == toolbarState.fullHeight) {
            animate = false
        }
    }
    //

    // refresh
    var isRefreshing by remember { mutableStateOf(false) }
    var enableRefresh by remember { mutableStateOf(false) }
    //
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {

                println("----- available.y: ${available.y}")
                println("----- enableRefresh: ${enableRefresh}")
                println("----- firstVisibleItemIndex: ${firstVisibleItemIndex}")
                println("----- firstVisibleItemScrollOffset: ${firstVisibleItemScrollOffset}")
                println("----- isSwipeInProgress: ${isSwipeInProgress}")
                println("---")

                if (isSwipeInProgress) {
                    toolbarState.isInteractingWithPullToRefresh = true
                    return Offset.Zero
                } else {
                    toolbarState.scrollTopLimitReached =
                        firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0

                    toolbarState.scrollOffset = toolbarState.scrollOffset - available.y

                    println("----- enableRefresh: ${enableRefresh}")
                    println("----- toolbarState.scrollOffset available.y: ${available.y}")
                    println("----- toolbarState.scrollOffset ssss: ${toolbarState.scrollOffset}")
                    println("----- firstVisibleItemIndex: ${firstVisibleItemIndex}")
                    println("----- firstVisibleItemScrollOffset: ${firstVisibleItemScrollOffset}")
                    println(".")
                    println(".")
                    println(".")

                    if (toolbarState.scrollOffset == toolbarState.fullHeight) {
                        toolbarState.isInteractingWithPullToRefresh = false
                    }

                    if (toolbarState.scrollTopLimitReached.not()) {
                        enableRefresh = false
                    } else if (enableRefresh.not()) {

                        enableRefresh =
                            toolbarState.scrollOffset < (toolbarState.collapsedHeight / 2) && toolbarState.scrollTopLimitReached
                        println("----- aa enableRefresh: ${enableRefresh}")
                    }

                    return Offset(0f, toolbarState.consumed)
                }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // hide total balance if the offset is 1/2 below its height
                if (toolbarState.scrollOffset > 0F &&
                    toolbarState.scrollOffset < toolbarState.collapsedHeight
                ) {
                    coroutineScopeAnim.launch {
                        animate = true
                        offsetY.snapTo(toolbarState.scrollOffset)
                        offsetY.animateTo(
                            targetValue = if (toolbarState.scrollOffset > toolbarState.collapsedHeight / 2) {
                                toolbarState.collapsedHeight
                            } else {
                                0F
                            },
                            animationSpec = tween(
                                durationMillis = 400
                            )
                        )
                    }
                }
                // if switcher is scrolled but still visible, snap to the top of it
                if (toolbarState.scrollOffset > toolbarState.collapsedHeight &&
                    toolbarState.scrollOffset < toolbarState.fullHeight
                ) {
                    coroutineScopeAnim.launch {
                        animate = true
                        offsetY.snapTo(toolbarState.scrollOffset)
                        offsetY.animateTo(
                            targetValue = toolbarState.collapsedHeight,
                            animationSpec = tween(
                                durationMillis = 400
                            )
                        )
                    }
                }

                enableRefresh = false
                toolbarState.isInteractingWithPullToRefresh = false
                //                println("----- minHeight: $minHeight")
                //                println("----- maxHeight: $maxHeight")
                //                println("----- toolbarState.scrollOffset: ${toolbarState.scrollOffset}")
                //                println("----- firstVisibleItemIndex: $firstVisibleItemIndex")
                //                println("----- firstVisibleItemScrollOffset: $firstVisibleItemScrollOffset")
                //                if (
                //                    firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
                //                ) {
                //                    enableRefresh = true
                //                } else {
                //                    enableRefresh = false
                //                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    // list y
    var headerBottomY by remember {
        mutableStateOf(0F)
    }
    //

    // background color
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
    //

    // header alpha
    val balanceLoadingAlpha by animateFloatAsState(
        targetValue = if (isRefreshing) 0F else 1F,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = 0
        )
    )
    var balanceScrollAlpha by remember {
        mutableStateOf(1F)
    }
    balanceScrollAlpha =
        (1 - (toolbarState.scrollOffset + (toolbarState.scrollOffset * 0.3F)) / toolbarState.collapsedHeight)
            .coerceIn(0F, 1F)

    var switcherAlpha by remember {
        mutableStateOf(1F)
    }
    switcherAlpha =
        (1 - (toolbarState.scrollOffset - toolbarState.collapsedHeight) / (toolbarState.fullHeight - toolbarState.collapsedHeight)).coerceIn(0F, 1F)
    println("----- switcherAlpha: ${switcherAlpha}")

    //

    // bottomnav animation
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
    //

    val navController = rememberNavController()

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        startColor,
                        endColor
                    )
                )
            )
    ) {
        val (statusBar, navBar, content, nav) = createRefs()

        Box(
            modifier = Modifier
                .constrainAs(content) {
                    start.linkTo(parent.start)
                    top.linkTo(statusBar.bottom)
                    end.linkTo(parent.end)
                    bottom.linkTo(navBar.top)
                    height = Dimension.fillToConstraints
                }

                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {

            /////// header
            ///////
            ///////
            ///////
            ///////
            ///////
            if (balanceSectionHeight > 0 && tabsSectionHeight > 0) {
                toolbarState.updateHeight(balanceSectionHeight, (balanceSectionHeight + tabsSectionHeight))
            }

            Column(modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    //                if (::toolbarState.isInitialized) {
                    translationY = -toolbarState.scrollOffset
                    //                }
                }
            ) {
                /////// balance
                ///////
                ///////
                TotalBalance(
                    modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isRefreshing) balanceLoadingAlpha else balanceScrollAlpha)
                    .onGloballyPositioned { coordinates ->
                        println("---- aa balanceSectionHeight ${coordinates.size.height}")
                        balanceSectionHeight = coordinates.size.height
                    },
                    balance = "$278,666.12"
                )

                /////// mode tabs
                ///////
                ///////
                ModeSwitcher(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(switcherAlpha)
                        .onGloballyPositioned { coordinates ->
                            println("---- aa tabsSectionHeight ${coordinates.size.height}")
                            tabsSectionHeight = coordinates.size.height
                        },
                    modes = listOf("Trading", "DeFi"),
                    onModeClicked = {
                        if (it == "Trading") {
                            shouldFlash = true
                            switch = true
                        } else if (it == "DeFi") {
                            shouldFlash = true
                            switch = false
                        }
                    },
                )
            }

            //////// content
            ////////
            ////////
            ////////
            ////////
            NavigationGraph(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = -toolbarState.scrollOffset + toolbarState.fullHeight
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                //                                scope.coroutineContext.cancelChildren()
                                coroutineScopeAnim.coroutineContext.cancelChildren()
                                animate = false
                            }
                        )
                    },
                navController = navController,
                enableRefresh = enableRefresh,
                indexedChanged = {
                    firstVisibleItemIndex = it.first
                    firstVisibleItemScrollOffset = it.second
                    isSwipeInProgress = it.third
                },
                refreshStarted = {
                    isRefreshing = true
                },
                refreshComplete = {
                    coroutineScopeAnim.launch {
                        if (toolbarState.scrollOffset < toolbarState.collapsedHeight) {
                            animate = true
                            offsetY.snapTo(toolbarState.scrollOffset)
                            offsetY.animateTo(
                                targetValue = toolbarState.collapsedHeight,
                                animationSpec = tween(
                                    durationMillis = 400
                                )
                            )
                        }
                        isRefreshing = false
                    }
                }
            )
        }

        BottomNavigationC(
            modifier = Modifier
                .wrapContentSize()
                .padding(34.dp)
                .constrainAs(nav) {
                    start.linkTo(parent.start)
                    bottom.linkTo(navBar.top)
                    end.linkTo(parent.end)
                }
                .offset {
                    IntOffset(
                        x = 0,
                        y = animateDown
                    )
                },
            navController
        ) {
        }

        // status bar
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Box(
            modifier = Modifier
                .constrainAs(statusBar) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                }
                .fillMaxWidth()
                .height(statusBarHeight)
        )

        // nav bar
        val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(
            modifier = Modifier
                .constrainAs(navBar) {
                    start.linkTo(parent.start)
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end)
                }
                .fillMaxWidth()
                .height(navBarHeight)
        )
    }
}