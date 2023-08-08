package com.blockchain.chrome.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.blockchain.analytics.Analytics
import com.blockchain.chrome.ChromeAnalyticsEvents
import com.blockchain.chrome.ChromeBackgroundColors
import com.blockchain.chrome.ChromeBottomNavigationItem
import com.blockchain.chrome.ChromeModeOptions
import com.blockchain.chrome.LocalChromePillProvider
import com.blockchain.chrome.MultiAppIntents
import com.blockchain.chrome.MultiAppNavigationEvent
import com.blockchain.chrome.MultiAppViewModel
import com.blockchain.chrome.MultiAppViewState
import com.blockchain.chrome.navigation.MultiAppBottomNavigationHost
import com.blockchain.chrome.toolbar.CollapsingToolbarState
import com.blockchain.chrome.toolbar.EnterAlwaysCollapsedState
import com.blockchain.chrome.toolbar.ScrollState
import com.blockchain.componentlib.alert.PillAlert
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.earn.navigation.EarnNavigation
import com.blockchain.extensions.safeLet
import com.blockchain.home.presentation.navigation.QrScanNavigation
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.Money
import kotlin.math.min
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.get

@Composable
private fun rememberToolbarState(modeSwitcherOptions: ChromeModeOptions): CollapsingToolbarState {
    val headerSectionHeightPx = with(LocalDensity.current) { 54.dp.toPx() }

    val bottomSectionHeight = if (modeSwitcherOptions is ChromeModeOptions.SingleSelection) {
        0
    } else {
        headerSectionHeightPx.toInt()
    }

    return rememberSaveable(saver = EnterAlwaysCollapsedState.Saver) {
        EnterAlwaysCollapsedState(
            topSectionHeight = headerSectionHeightPx.toInt(),
            bottomSectionHeight = bottomSectionHeight
        )
    }
}

@Composable
fun MultiAppChrome(
    viewModel: MultiAppViewModel,
    analytics: Analytics = get(),
    onModeLongClicked: (WalletMode) -> Unit,
    showWalletIntro: (WalletMode) -> Unit,
    startPhraseRecovery: () -> Unit,
    showAppRating: () -> Unit,
    qrScanNavigation: QrScanNavigation,
    graphNavController: NavController,
    openExternalUrl: (url: String) -> Unit,
    openNftHelp: () -> Unit,
    processAnnouncementUrl: (String) -> Unit,
    openNftDetail: (nftId: String, address: String, pageKey: String?) -> Unit,
    earnNavigation: EarnNavigation,
) {
    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(MultiAppIntents.LoadData)
        onDispose { }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    val navEventsFlowLifecycleAware = remember(viewModel.navigationEventFlow, lifecycleOwner) {
        viewModel.navigationEventFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    LaunchedEffect(key1 = viewModel) {
        navEventsFlowLifecycleAware.collectLatest {
            when (it) {
                is MultiAppNavigationEvent.WalletIntro -> showWalletIntro(it.walletMode)
                is MultiAppNavigationEvent.AppRating -> showAppRating()
            }
        }
    }

    val viewState: MultiAppViewState by viewModel.viewState.collectAsStateLifecycleAware()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    safeLet(
        viewState.modeSwitcherOptions,
        viewState.selectedMode,
        viewState.backgroundColors,
        viewState.bottomNavigationItems
    ) { modeSwitcherOptions, selectedMode, backgroundColors, bottomNavigationItems ->
        MultiAppChromeScreen(
            analytics = analytics,
            statusBarHeight = statusBarHeight,
            navBarHeight = navBarHeight,
            modeSwitcherOptions = modeSwitcherOptions,
            selectedMode = selectedMode,
            backgroundColors = backgroundColors,
            balance = viewState.totalBalance,
            shouldRevealBalance = viewState.shouldRevealBalance,
            bottomNavigationItems = bottomNavigationItems.toImmutableList(),
            selectedBottomNavigationItem = viewState.selectedBottomNavigationItem,
            onBottomNavigationItemSelected = { navItem ->
                viewModel.onIntent(MultiAppIntents.BottomNavigationItemSelected(navItem))
            },
            onModeSelected = { walletMode ->
                viewModel.onIntent(MultiAppIntents.WalletModeSelected(walletMode))
            },
            onModeLongClicked = onModeLongClicked,
            graphNavController = graphNavController,
            qrScanNavigation = qrScanNavigation,
            onBalanceRevealed = {
                viewModel.onIntent(MultiAppIntents.BalanceRevealed)
            },
            startPhraseRecovery = {
                startPhraseRecovery()
            },
            openExternalUrl = openExternalUrl,
            openNftHelp = openNftHelp,
            openNftDetail = openNftDetail,
            earnNavigation = earnNavigation,
            processAnnouncementUrl = processAnnouncementUrl,
        )
    }
}

@Composable
fun MultiAppChromeScreen(
    analytics: Analytics,
    statusBarHeight: Dp,
    navBarHeight: Dp,
    modeSwitcherOptions: ChromeModeOptions,
    selectedMode: WalletMode,
    backgroundColors: ChromeBackgroundColors,
    balance: DataResource<Money>,
    shouldRevealBalance: Boolean,
    bottomNavigationItems: ImmutableList<ChromeBottomNavigationItem>,
    selectedBottomNavigationItem: ChromeBottomNavigationItem,
    onBottomNavigationItemSelected: (ChromeBottomNavigationItem) -> Unit,
    onModeSelected: (WalletMode) -> Unit,
    onModeLongClicked: (WalletMode) -> Unit,
    qrScanNavigation: QrScanNavigation,
    onBalanceRevealed: () -> Unit,
    graphNavController: NavController,
    startPhraseRecovery: () -> Unit,
    openExternalUrl: (url: String) -> Unit,
    openNftHelp: () -> Unit,
    openNftDetail: (nftId: String, address: String, pageKey: String?) -> Unit,
    earnNavigation: EarnNavigation,
    processAnnouncementUrl: (String) -> Unit,
) {
    val toolbarState = rememberToolbarState(modeSwitcherOptions)
    val navController = rememberNavController()

    LaunchedEffect(selectedBottomNavigationItem) {
        navController.navigate(selectedBottomNavigationItem.route) {
            navController.graph.startDestinationRoute?.let { screen_route ->
                popUpTo(screen_route) {
                    saveState = true
                }
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // //////////////////////////////////////////////
    // update vm selected bottom nav item on back press if needed
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val navControllerBottomNav = bottomNavigationItems.firstOrNull { it.route == navBackStackEntry?.destination?.route }
    LaunchedEffect(key1 = navControllerBottomNav) {
        navControllerBottomNav?.let {
            onBottomNavigationItemSelected(it)
        }
    }
    // //////////////////////////////////////////////

    // //////////////////////////////////////////////
    // snap header views depending on scroll position
    val coroutineScopeSnaps = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    var animateSnap by remember { mutableStateOf(false) }

    if (animateSnap) {
        toolbarState.scrollOffset = offsetY.value

        if (toolbarState.scrollOffset == toolbarState.fullCollapsedOffset) {
            animateSnap = false
        }
    }

    suspend fun updateOffset(targetValue: Float) {
        toolbarState.isAutoScrolling = true
        animateSnap = true
        offsetY.snapTo(toolbarState.scrollOffset)
        offsetY.animateTo(
            targetValue = targetValue,
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION
            )
        )
        animateSnap = false
        toolbarState.isAutoScrolling = false
    }

    fun updateOffsetNoAnimation(targetValue: Float) {
        toolbarState.scrollOffset = targetValue
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
            if (toolbarState.scrollOffset < toolbarState.halfCollapsedOffset) {
                updateOffset(targetValue = toolbarState.halfCollapsedOffset)
            }
            isRefreshing = false
        }
    }
    // //////////////////////////////////////////////

    // //////////////////////////////////////////////
    // header alpha
    val balanceLoadingAlpha by animateFloatAsState(
        targetValue = if (isRefreshing) 0F else 1F,
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION
        )
    )
    // //////////////////////////////////////////////

    // //////////////////////////////////////////////
    // first launch
    var firstLaunch by remember { mutableStateOf(true) }
    if (firstLaunch) {
        updateOffsetNoAnimation(targetValue = toolbarState.halfCollapsedOffset)
        firstLaunch = false
    }

    val coroutineScopeBalanceReveal = rememberCoroutineScope()

    /**
     * true if the balance is the target to show.
     * once it's shown and [REVEAL_DELAY_MS] is over,
     * it will be false to revert back to the switcher
     */
    var isRevealingTargetBalance by remember { mutableStateOf(false) }

    val balanceRevealAlpha by animateFloatAsState(
        targetValue = if (isRevealingTargetBalance) 1F else 0F,
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION / 2
        )
    )
    val switcherRevealAlpha by animateFloatAsState(
        targetValue = if (isRevealingTargetBalance) 0F else 1F,
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION / 2
        ),
        finishedListener = {
            // reveal in progress and switcher is back to fully visible
            // -> all done
            if (toolbarState.isBalanceRevealInProgress && it == 1F) {
                toolbarState.isBalanceRevealInProgress = false
            }
        }
    )

    /**
     * default to true to show the balance when data is available
     *
     * will be false if user starts scrolling before data is available.
     * if the balance is revealed and user scrolls past the full header
     */
    fun revealSwitcher() {
        isRevealingTargetBalance = false
    }

    fun revealBalance() {
        if (toolbarState.isBalanceRevealInProgress.not()) {
            toolbarState.isBalanceRevealInProgress = true
            isRevealingTargetBalance = true

            onBalanceRevealed()

            coroutineScopeBalanceReveal.launch {
                delay(REVEAL_DELAY_MS)
                revealSwitcher()
            }
        }
    }
    // //////////////////////////////////////////////

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (toolbarState.isPullToRefreshSwipeInProgress) {
                    toolbarState.isInteractingWithPullToRefresh = true
                    // let pull to refresh consume the scroll
                    return Offset.Zero
                } else {
                    toolbarState.scrollOffset = toolbarState.scrollOffset - available.y

                    if (toolbarState.scrollOffset == toolbarState.fullCollapsedOffset) {
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
                            toolbarState.scrollOffset < (toolbarState.halfCollapsedOffset / 2)

                        enablePullToRefresh = pullToRefreshEnabledThreshold && toolbarState.scrollTopLimitReached
                    }

                    return Offset(0f, toolbarState.consumed)
                }
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (isRevealingTargetBalance &&
                    (
                        toolbarState.scrollState == ScrollState.Up &&
                            toolbarState.scrollOffset <= toolbarState.halfCollapsedOffset
                        ) ||
                    (
                        toolbarState.scrollState == ScrollState.Down &&
                            toolbarState.scrollOffset >= toolbarState.fullCollapsedOffset
                        )
                ) {
                    coroutineScopeBalanceReveal.coroutineContext.cancelChildren()
                    if (isRevealingTargetBalance) {
                        isRevealingTargetBalance = false
                    }
                }
                return super.onPostScroll(consumed, available, source)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // hide total balance if the offset is 1/2 below its height
                if (toolbarState.scrollOffset > 0F &&
                    toolbarState.scrollOffset < toolbarState.halfCollapsedOffset
                ) {
                    coroutineScopeSnaps.launch {
                        updateOffset(
                            targetValue = if (toolbarState.scrollOffset > toolbarState.halfCollapsedOffset / 2) {
                                toolbarState.halfCollapsedOffset
                            } else {
                                0F
                            }
                        )
                    }
                }
                // hide switcher if the offset is 1/2 below its height
                if (toolbarState.scrollOffset > toolbarState.halfCollapsedOffset &&
                    toolbarState.scrollOffset < toolbarState.fullCollapsedOffset
                ) {
                    val halfSwitcherHeight = (toolbarState.fullCollapsedOffset + toolbarState.halfCollapsedOffset) / 2

                    coroutineScopeSnaps.launch {
                        updateOffset(
                            targetValue = if (toolbarState.scrollOffset < halfSwitcherHeight) {
                                // snap to top of it
                                toolbarState.halfCollapsedOffset
                            } else {
                                // snap below (hide it)
                                toolbarState.fullCollapsedOffset
                            }
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
    val backgroundStartColor by animateColorAsState(
        targetValue = backgroundColors.startColor,
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION
        )
    )
    val backgroundEndColor by animateColorAsState(
        targetValue = backgroundColors.endColor,
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION
        )
    )
    // //////////////////////////////////////////////

    // //////////////////////////////////////////////
    // fix scroll position for new screens
    var verifyHeaderPositionForNewScreen by remember { mutableStateOf(false) }
    fun verifyAndCollapseHeaderForNewScreen() {
        if (verifyHeaderPositionForNewScreen &&
            toolbarState.scrollTopLimitReached.not() &&
            toolbarState.scrollOffset == 0F && animateSnap.not()
        ) {
            coroutineScopeSnaps.launch {
                updateOffset(targetValue = toolbarState.halfCollapsedOffset)
            }
        }
        verifyHeaderPositionForNewScreen = false
    }

    // //////////////////////////////////////////////
    // bottomnav animation
    var currentBottomNavigationItems by remember { mutableStateOf(emptyList<ChromeBottomNavigationItem>()) }

    var bottomNavigationVisible by remember { mutableStateOf(true) }

    if (bottomNavigationVisible && currentBottomNavigationItems.toSet() != bottomNavigationItems.toSet()) {
        currentBottomNavigationItems = bottomNavigationItems
    }

    val bottomNavOffsetY by animateIntAsState(
        targetValue = if (bottomNavigationVisible) 0 else 300,
        finishedListener = {
            if (bottomNavigationVisible.not()) {
                bottomNavigationVisible = true
            }
        },
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION
        )
    )
    // //////////////////////////////////////////////

    // //////////////////////////////////////////////
    // pill alert
    val pillAlert by LocalChromePillProvider.current.alert.collectAsStateLifecycleAware(null)
    var showPill by remember { mutableStateOf(false) }
    LaunchedEffect(pillAlert) {
        pillAlert?.let {
            showPill = true
            delay(REVEAL_DELAY_MS)
            showPill = false
        }
    }
    val pillAlertOffsetY by animateFloatAsState(
        targetValue = if (showPill) 0F else -300F,
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION
        )
    )

    fun pillAlphaInterpolator(value: Float): Float {
        val x1 = 0f
        val x2 = -300f
        val y1 = 1f
        val y2 = 0.5f
        return (value - x1) * (y2 - y1) / (x2 - x1) + y1
    }
    // //////////////////////////////////////////////

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
            // /// header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = -toolbarState.scrollOffset
                    }
            ) {
                // ///// balance
                TotalBalance(
                    modifier = Modifier
                        .height(54.dp)
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = if (isRefreshing) {
                                balanceLoadingAlpha
                            } else {
                                toolbarState.balanceScrollAlpha(firstLaunch)
                            }
                        },
                    balance = balance
                )

                // ///// mode tabs
                when (modeSwitcherOptions) {
                    is ChromeModeOptions.MultiSelection -> {
                        Box(
                            modifier = Modifier
                                .height(54.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            val revealPadding = remember { 40 }

                            TotalBalance(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        translationY = (balanceRevealAlpha - 1) * revealPadding
                                        alpha = min(balanceRevealAlpha, toolbarState.switcherScrollAlpha())
                                    },
                                balance = balance
                            )

                            if (
                                balance is DataResource.Data && shouldRevealBalance &&
                                toolbarState.isBalanceRevealInProgress.not()
                            ) {
                                revealBalance()
                            }

                            ModeSwitcher(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        translationY = -1 * ((switcherRevealAlpha - 1) * revealPadding)
                                        alpha = if (toolbarState.isBalanceRevealInProgress) {
                                            min(switcherRevealAlpha, toolbarState.switcherScrollAlpha())
                                        } else {
                                            toolbarState.switcherScrollAlpha()
                                        }
                                    },
                                modes = modeSwitcherOptions.modes.toImmutableList(),
                                selectedMode = selectedMode,
                                onModeClicked = { walletMode ->
                                    if (toolbarState.isBalanceRevealInProgress.not()) {
                                        stopRefresh()
                                        bottomNavigationVisible = false
                                        onModeSelected(walletMode)
                                    }

                                    analytics.logEvent(ChromeAnalyticsEvents.ModeClicked(walletMode))
                                },
                                onModeLongClicked = { walletMode ->
                                    onModeLongClicked(walletMode)

                                    analytics.logEvent(ChromeAnalyticsEvents.ModeLongClicked(walletMode))
                                }
                            )
                        }
                    }

                    is ChromeModeOptions.SingleSelection -> {
                        // n/a
                    }
                }
            }

            // ////// content
            // the screen can look jumpy at first launch since views positions are initialized dynamically
            // content will be hidden at first until we have the view heights (toolbarState.offsetValuesSet)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = -toolbarState.scrollOffset + toolbarState.fullCollapsedOffset
                    }
                    .background(
                        color = AppColors.background,
                        shape = RoundedCornerShape(
                            topStart = AppTheme.dimensions.standardSpacing,
                            topEnd = AppTheme.dimensions.standardSpacing
                        )
                    )
            )

            AnimatedVisibility(
                visible = toolbarState.offsetValuesSet,
                enter = fadeIn(tween(durationMillis = ANIMATION_DURATION / 2)),
                exit = fadeOut(tween(durationMillis = ANIMATION_DURATION / 2))
            ) {
                MultiAppBottomNavigationHost(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = -toolbarState.scrollOffset + toolbarState.fullCollapsedOffset
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    // ignore the reveal if user interacts with the
                                    // content already before getting the balance
                                    if (toolbarState.isBalanceRevealInProgress.not()) {
                                        onBalanceRevealed()
                                        coroutineScopeBalanceReveal.coroutineContext.cancelChildren()
                                    }

                                    coroutineScopeSnaps.coroutineContext.cancelChildren()
                                    animateSnap = false
                                    verifyHeaderPositionForNewScreen = false
                                    toolbarState.isAutoScrolling = false
                                }
                            )
                        },
                    navControllerProvider = { navController },
                    enableRefresh = enablePullToRefresh,
                    updateScrollInfo = { (navItem, listStateInfo) ->
                        toolbarState.scrollTopLimitReached = listStateInfo.isFirstItemVisible &&
                            listStateInfo.isFirstVisibleItemOffsetZero

                        toolbarState.isPullToRefreshSwipeInProgress = listStateInfo.isSwipeInProgress

                        if (verifyHeaderPositionForNewScreen && selectedBottomNavigationItem == navItem) {
                            verifyAndCollapseHeaderForNewScreen()
                        }
                    },
                    selectedNavigationItem = selectedBottomNavigationItem,
                    refreshStarted = {
                        isRefreshing = true
                    },
                    refreshComplete = {
                        stopRefresh()
                    },
                    navController = graphNavController,
                    qrScanNavigation = qrScanNavigation,
                    startPhraseRecovery = startPhraseRecovery,
                    openExternalUrl = openExternalUrl,
                    openNftHelp = openNftHelp,
                    openNftDetail = openNftDetail,
                    earnNavigation = earnNavigation,
                    processAnnouncementUrl = processAnnouncementUrl,
                    navigateToMode = {
                        stopRefresh()
                        bottomNavigationVisible = false
                        onModeSelected(it)
                    }
                )
            }

            // error pill
            pillAlert?.let {
                PillAlert(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(AppTheme.dimensions.tinySpacing)
                        .graphicsLayer {
                            translationY = pillAlertOffsetY
                            alpha = pillAlphaInterpolator(pillAlertOffsetY)
                        },
                    config = it
                )
            }
        }

        // ////// bottom nav
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
            navigationItems = currentBottomNavigationItems.toImmutableList(),
            selectedNavigationItem = selectedBottomNavigationItem,
            onSelected = {
                if (isRefreshing) {
                    stopRefresh()
                } else {
                    onBottomNavigationItemSelected(it)
                    verifyHeaderPositionForNewScreen = true
                }
            }
        )

        // we have to reserve spaces for the statusbar and nav bar because the screen can draw on them
        // so we can have custom gradient status bar

        // status bar
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
