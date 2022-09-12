package piuk.blockchain.android.ui.multiapp.composable

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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.compose.rememberNavController
import com.blockchain.componentlib.theme.END_DEFI
import com.blockchain.componentlib.theme.END_TRADING
import com.blockchain.componentlib.theme.START_DEFI
import com.blockchain.componentlib.theme.START_TRADING
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import piuk.blockchain.android.ui.multiapp.toolbar.CollapsingToolbarState
import piuk.blockchain.android.ui.multiapp.toolbar.EnterAlwaysCollapsedState

@Composable
private fun rememberToolbarState(): CollapsingToolbarState {
    return rememberSaveable(saver = EnterAlwaysCollapsedState.Saver) {
        // initialize with minHeight:0 maxHeight:0
        // the size will be calculated at runtime after drawing to get the real view heights
        EnterAlwaysCollapsedState(0, 0)
    }
}

val modes = listOf("Trading", "DeFi")

@Composable
fun MultiAppDashboard() {
    var balanceSectionHeight by remember {
        mutableStateOf(0)
    }
    var tabsSectionHeight by remember {
        mutableStateOf(0)
    }

    val toolbarState = rememberToolbarState()

    /**
     * if the screen is currently trying pull to refresh
     * i.e. is pulling and seeing the loading indicator
     * (refreshing is not triggered yet at this point, just the interaction swipe up and down)
     */
    var isPullToRefreshSwipeInProgress by remember {
        mutableStateOf(false)
    }

    // //////////////////////////////////////////////
    // snap header views depending on scroll position
    val coroutineScopeSnaps = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    var animateSnap by remember { mutableStateOf(false) }

    if (animateSnap) {
        toolbarState.scrollOffset = offsetY.value
        if (toolbarState.scrollOffset == toolbarState.fullHeight) {
            animateSnap = false
        }
    }
    // //////////////////////////////////////////////

    // //////////////////////////////////////////////
    // pull to refresh
    var isRefreshing by remember { mutableStateOf(false) }

    /**
     * pull to refresh needs to be enabled/disabled based on scroll position
     * because it also consumes nested scroll events
     */
    var enablePullToRefresh by remember { mutableStateOf(false) }

    fun stopRefresh() {
        coroutineScopeSnaps.launch {
            if (toolbarState.scrollOffset < toolbarState.collapsedHeight) {
                animateSnap = true
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
    // //////////////////////////////////////////////

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {

                if (isPullToRefreshSwipeInProgress) {
                    toolbarState.isInteractingWithPullToRefresh = true
                    // let pull to refresh consume the scroll
                    return Offset.Zero
                } else {
                    toolbarState.scrollOffset = toolbarState.scrollOffset - available.y

                    if (toolbarState.scrollOffset == toolbarState.fullHeight) {
                        // disable pull to refresh when user scrolls past the full header
                        // to be able to lock again at the ModeSwitcher level
                        toolbarState.isInteractingWithPullToRefresh = false
                    }

                    if (toolbarState.scrollTopLimitReached.not()) {
                        // if the current screen is not scrolled all the way to the top
                        // disable pull to refresh so this nested scroll can consume all the events
                        enablePullToRefresh = false
                    } else if (enablePullToRefresh.not()) {
                        val pullToRefreshEnabledThreshold =
                            toolbarState.scrollOffset < (toolbarState.collapsedHeight / 2)

                        enablePullToRefresh = pullToRefreshEnabledThreshold && toolbarState.scrollTopLimitReached
                    }

                    return Offset(0f, toolbarState.consumed)
                }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // hide total balance if the offset is 1/2 below its height
                if (toolbarState.scrollOffset > 0F &&
                    toolbarState.scrollOffset < toolbarState.collapsedHeight
                ) {
                    coroutineScopeSnaps.launch {
                        animateSnap = true
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
                    coroutineScopeSnaps.launch {
                        animateSnap = true
                        offsetY.snapTo(toolbarState.scrollOffset)
                        offsetY.animateTo(
                            targetValue = toolbarState.collapsedHeight,
                            animationSpec = tween(
                                durationMillis = 400
                            )
                        )
                    }
                }

                enablePullToRefresh = false
                toolbarState.isInteractingWithPullToRefresh = false

                return super.onPostFling(consumed, available)
            }
        }
    }

    // //////////////////////////////////////////////
    // background color
    var selectedMode by remember { mutableStateOf(modes.first()) }
    val backgroundStartColor by animateColorAsState(
        targetValue = if (selectedMode == modes.first()) START_TRADING else START_DEFI,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = 0
        )
    )
    val backgroundEndColor by animateColorAsState(
        targetValue = if (selectedMode == modes.first()) END_TRADING else END_DEFI,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = 0
        )
    )
    // //////////////////////////////////////////////

    // //////////////////////////////////////////////
    // header alpha
    val balanceLoadingAlpha by animateFloatAsState(
        targetValue = if (isRefreshing) 0F else 1F,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = 0
        )
    )
    var balanceScrollAlpha by remember { mutableStateOf(1F) }
    balanceScrollAlpha =
        (1 - (toolbarState.scrollOffset + (toolbarState.scrollOffset * 0.3F)) / toolbarState.collapsedHeight)
            .coerceIn(0F, 1F)

    var switcherScrollAlpha by remember { mutableStateOf(1F) }
    switcherScrollAlpha =
        (1 - (toolbarState.scrollOffset - toolbarState.collapsedHeight) / (toolbarState.fullHeight - toolbarState.collapsedHeight)).coerceIn(
            0F, 1F
        )
    // //////////////////////////////////////////////

    // //////////////////////////////////////////////
    // bottomnav animation
    // todo refactor
    var trading by remember { mutableStateOf(true) }
    var animateBottomNav by remember { mutableStateOf(false) }
    val bottomNavOffsetY by animateIntAsState(
        targetValue = if (animateBottomNav) 300 else 0,
        finishedListener = {
            if (animateBottomNav) {
                trading = trading.not()
                animateBottomNav = false
            }
        },
        animationSpec = tween(
            durationMillis = 200,
            delayMillis = 0
        )
    )
    // //////////////////////////////////////////////

    val navController = rememberNavController()

    // this container has the following format
    // -> Space for the toolbar
    // -> collapsable header
    //    -> total balance
    //    -> mode switcher tabs
    // -> main screen navhost content
    // -> floating bottom navigation
    // -> Space for the native android navigation
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        backgroundStartColor,
                        backgroundEndColor
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
            if (balanceSectionHeight > 0 && tabsSectionHeight > 0) {
                toolbarState.updateHeight(balanceSectionHeight, (balanceSectionHeight + tabsSectionHeight))
            }

            Column(modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = -toolbarState.scrollOffset
                }
            ) {
                /////// balance
                TotalBalance(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isRefreshing) balanceLoadingAlpha else balanceScrollAlpha)
                        .onGloballyPositioned { coordinates ->
                            balanceSectionHeight = coordinates.size.height
                        },
                    balance = "$278,666.12"
                )

                /////// mode tabs
                ModeSwitcher(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(switcherScrollAlpha)
                        .onGloballyPositioned { coordinates ->
                            tabsSectionHeight = coordinates.size.height
                        },
                    modes = modes,
                    onModeClicked = {
                        stopRefresh()

                        if (it == modes[0]) {
                            animateBottomNav = true
                            selectedMode = modes[0]
                        } else if (it == modes[1]) {
                            animateBottomNav = true
                            selectedMode = modes[1]
                        }
                    },
                )
            }

            //////// content
            MultiAppNavigationGraph(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = -toolbarState.scrollOffset + toolbarState.fullHeight
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                coroutineScopeSnaps.coroutineContext.cancelChildren()
                                animateSnap = false
                            }
                        )
                    },
                navController = navController,
                enableRefresh = enablePullToRefresh,
                updateScrollInfo = { (firstVisibleItemIndex, firstVisibleItemScrollOffset, isSwipeInProgress) ->
                    toolbarState.scrollTopLimitReached = firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
                    isPullToRefreshSwipeInProgress = isSwipeInProgress
                },
                refreshStarted = {
                    isRefreshing = true
                },
                refreshComplete = {
                   stopRefresh()
                }
            )
        }

        //////// bottom nav
        MultiAppBottomNavigation(
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
                        y = bottomNavOffsetY
                    )
                },
            navController
        ) {
            if (isRefreshing) {
                stopRefresh()
            }
        }

        // we have to reserve spaces for the statusbar and nav bar because the screen can draw on them
        // so we can have custom gradient status bar

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