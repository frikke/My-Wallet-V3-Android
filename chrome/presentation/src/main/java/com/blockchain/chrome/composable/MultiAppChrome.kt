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
import androidx.compose.ui.graphics.Color
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
import androidx.navigation.compose.rememberNavController
import com.blockchain.chrome.ChromeBackgroundColors
import com.blockchain.chrome.ChromeBottomNavigationItem
import com.blockchain.chrome.ChromeModeOptions
import com.blockchain.chrome.MultiAppIntents
import com.blockchain.chrome.MultiAppNavigationEvent
import com.blockchain.chrome.MultiAppViewModel
import com.blockchain.chrome.MultiAppViewState
import com.blockchain.chrome.navigation.MultiAppBottomNavigationHost
import com.blockchain.chrome.toolbar.CollapsingToolbarState
import com.blockchain.chrome.toolbar.EnterAlwaysCollapsedState
import com.blockchain.chrome.toolbar.ScrollState
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.home.presentation.navigation.QrScanNavigation
import com.blockchain.home.presentation.navigation.SettingsNavigation
import com.blockchain.home.presentation.navigation.SupportNavigation
import com.blockchain.koin.payloadScope
import com.blockchain.prices.navigation.PricesNavigation
import com.blockchain.walletmode.WalletMode
import kotlin.math.min
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

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
    viewModel: MultiAppViewModel = getViewModel(scope = payloadScope),
    onModeLongClicked: (WalletMode) -> Unit,
    startDefiOnboarding: (walletActivationRequired: Boolean) -> Unit,
    openCryptoAssets: () -> Unit,
    assetActionsNavigation: AssetActionsNavigation,
    settingsNavigation: SettingsNavigation,
    pricesNavigation: PricesNavigation,
    qrScanNavigation: QrScanNavigation,
    supportNavigation: SupportNavigation,
    openActivity: () -> Unit,
    openActivityDetail: (String, WalletMode) -> Unit,
    openReferral: () -> Unit,
    openFiatActionDetail: (String) -> Unit,
    openMoreQuickActions: () -> Unit,
) {
    DisposableEffect(key1 = viewModel) {
        viewModel.viewCreated(ModelConfigArgs.NoArgs)
        onDispose { }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    val navEventsFlowLifecycleAware = remember(viewModel.navigationEventFlow, lifecycleOwner) {
        viewModel.navigationEventFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    LaunchedEffect(key1 = viewModel) {
        navEventsFlowLifecycleAware.collectLatest {
            when (it) {
                is MultiAppNavigationEvent.PhraseRecovery -> startDefiOnboarding(it.walletActivationRequired)
            }
        }
    }

    val viewState: MultiAppViewState by viewModel.viewState.collectAsStateLifecycleAware()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    if (statusBarHeight > 0.dp && navBarHeight > 0.dp) {
        MultiAppChromeScreen(
            statusBarHeight = statusBarHeight,
            navBarHeight = navBarHeight,
            modeSwitcherOptions = viewState.modeSwitcherOptions,
            selectedMode = viewState.selectedMode,
            backgroundColors = viewState.backgroundColors,
            balance = viewState.totalBalance,
            shouldRevealBalance = viewState.shouldRevealBalance,
            bottomNavigationItems = viewState.bottomNavigationItems.toImmutableList(),
            onModeSelected = { walletMode ->
                viewModel.onIntent(MultiAppIntents.WalletModeChangeRequested(walletMode))
            },
            onModeLongClicked = onModeLongClicked,
            openCryptoAssets = openCryptoAssets,
            openActivity = openActivity,
            openActivityDetail = openActivityDetail,
            openReferral = openReferral,
            openFiatActionDetail = openFiatActionDetail,
            openMoreQuickActions = openMoreQuickActions,
            assetActionsNavigation = assetActionsNavigation,
            settingsNavigation = settingsNavigation,
            pricesNavigation = pricesNavigation,
            qrScanNavigation = qrScanNavigation,
            supportNavigation = supportNavigation,
            onBalanceRevealed = {
                viewModel.onIntent(MultiAppIntents.BalanceRevealed)
            }
        )
    }
}

@Composable
fun MultiAppChromeScreen(
    statusBarHeight: Dp,
    navBarHeight: Dp,
    modeSwitcherOptions: ChromeModeOptions,
    selectedMode: WalletMode,
    backgroundColors: ChromeBackgroundColors,
    balance: DataResource<String>,
    shouldRevealBalance: Boolean,
    bottomNavigationItems: ImmutableList<ChromeBottomNavigationItem>,
    onModeSelected: (WalletMode) -> Unit,
    onModeLongClicked: (WalletMode) -> Unit,
    openCryptoAssets: () -> Unit,
    assetActionsNavigation: AssetActionsNavigation,
    settingsNavigation: SettingsNavigation,
    pricesNavigation: PricesNavigation,
    qrScanNavigation: QrScanNavigation,
    supportNavigation: SupportNavigation,
    openActivity: () -> Unit,
    openActivityDetail: (String, WalletMode) -> Unit,
    openReferral: () -> Unit,
    openMoreQuickActions: () -> Unit,
    openFiatActionDetail: (String) -> Unit,
    onBalanceRevealed: () -> Unit
) {
    val toolbarState = rememberToolbarState(modeSwitcherOptions)

    var selectedNavigationItem by remember { mutableStateOf(bottomNavigationItems.first()) }

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

    fun updateOffsetScoped(targetValue: Float, delay: Long = 0L) {
        coroutineScopeSnaps.launch {
            delay(delay)
            updateOffset(targetValue)
        }
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
     * once it's shown and [REVEAL_BALANCE_DELAY_MS] is over,
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
                delay(REVEAL_BALANCE_DELAY_MS)
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
                // if switcher is scrolled but still visible, snap to the top of it
                if (toolbarState.scrollOffset > toolbarState.halfCollapsedOffset &&
                    toolbarState.scrollOffset < toolbarState.fullCollapsedOffset
                ) {
                    coroutineScopeSnaps.launch {
                        updateOffset(targetValue = toolbarState.halfCollapsedOffset)
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

    // //////////////////////////////////////////////
    // show and hide balance on first launch
    var hideBalanceAfterInitialValue by remember { mutableStateOf(false) }
    fun showAndHideBalanceOnFirstLaunch() {
        coroutineScopeSnaps.launch {
            if (balance is DataResource.Data && hideBalanceAfterInitialValue.not() && toolbarState.offsetValuesSet) {
                hideBalanceAfterInitialValue = true

                updateOffset(0F)

                delay(2000L)

                updateOffset(toolbarState.halfCollapsedOffset)
            }
        }
    }
    // //////////////////////////////////////////////

    // //////////////////////////////////////////////
    // bottomnav animation
    var currentBottomNavigationItems by remember { mutableStateOf(bottomNavigationItems) }
    var bottomNavigationVisible by remember { mutableStateOf(true) }
    val bottomNavOffsetY by animateIntAsState(
        targetValue = if (bottomNavigationVisible) 0 else 300,
        finishedListener = {
            if (bottomNavigationVisible.not()) {
                bottomNavigationVisible = true
                currentBottomNavigationItems = bottomNavigationItems
            }
        },
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION
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
                                },
                                onModeLongClicked = onModeLongClicked
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
                        color = Color(0XFFF1F2F7),
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

                        if (verifyHeaderPositionForNewScreen && selectedNavigationItem == navItem) {
                            verifyAndCollapseHeaderForNewScreen()
                        }
                    },
                    selectedNavigationItem = selectedNavigationItem,
                    refreshStarted = {
                        isRefreshing = true
                    },
                    refreshComplete = {
                        stopRefresh()
                    },
                    openCryptoAssets = openCryptoAssets,
                    openActivity = openActivity,
                    openActivityDetail = openActivityDetail,
                    openReferral = openReferral,
                    openMoreQuickActions = openMoreQuickActions,
                    openFiatActionDetail = openFiatActionDetail,
                    assetActionsNavigation = assetActionsNavigation,
                    settingsNavigation = settingsNavigation,
                    pricesNavigation = pricesNavigation,
                    qrScanNavigation = qrScanNavigation,
                    supportNavigation = supportNavigation,
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
            navControllerProvider = { navController },
            navigationItems = currentBottomNavigationItems.toImmutableList(),
        ) {
            if (isRefreshing) {
                stopRefresh()
            } else {
                selectedNavigationItem = it
                verifyHeaderPositionForNewScreen = true
            }
        }

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
