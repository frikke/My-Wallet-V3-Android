package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelStoreOwner
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.chrome.LocalNavControllerProvider
import com.blockchain.chrome.navigation.AssetActionsNavigation
import com.blockchain.chrome.navigation.LocalAssetActionsNavigationProvider
import com.blockchain.chrome.navigation.LocalRecurringBuyNavigationProvider
import com.blockchain.chrome.navigation.LocalSupportNavigationProvider
import com.blockchain.coincore.AssetAction
import com.blockchain.commonarch.presentation.mvi_v2.compose.NavArgument
import com.blockchain.commonarch.presentation.mvi_v2.compose.navigate
import com.blockchain.componentlib.alert.AlertType
import com.blockchain.componentlib.alert.CardAlert
import com.blockchain.componentlib.card.CardButton
import com.blockchain.componentlib.chrome.MenuOptionsScreen
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.permissions.RuntimePermission
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.data.DataResource
import com.blockchain.data.dataOrElse
import com.blockchain.data.doOnData
import com.blockchain.data.map
import com.blockchain.data.toImmutableList
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.home.handhold.HandholdTask
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.accouncement.AnnouncementsIntent
import com.blockchain.home.presentation.accouncement.AnnouncementsViewModel
import com.blockchain.home.presentation.accouncement.AnnouncementsViewState
import com.blockchain.home.presentation.accouncement.LocalAnnouncementType
import com.blockchain.home.presentation.accouncement.composable.LocalAnnouncements
import com.blockchain.home.presentation.accouncement.composable.StackedAnnouncements
import com.blockchain.home.presentation.activity.list.ActivityIntent
import com.blockchain.home.presentation.activity.list.ActivityViewState
import com.blockchain.home.presentation.activity.list.custodial.CustodialActivityViewModel
import com.blockchain.home.presentation.activity.list.privatekey.PrivateKeyActivityViewModel
import com.blockchain.home.presentation.allassets.AssetsIntent
import com.blockchain.home.presentation.allassets.AssetsViewModel
import com.blockchain.home.presentation.allassets.AssetsViewState
import com.blockchain.home.presentation.dapps.HomeDappsIntent
import com.blockchain.home.presentation.dapps.HomeDappsViewModel
import com.blockchain.home.presentation.dapps.HomeDappsViewState
import com.blockchain.home.presentation.dashboard.DashboardAnalyticsEvents
import com.blockchain.home.presentation.failedbalances.FailedBalancesIntent
import com.blockchain.home.presentation.failedbalances.FailedBalancesViewModel
import com.blockchain.home.presentation.failedbalances.FailedBalancesViewState
import com.blockchain.home.presentation.handhold.HandholdIntent
import com.blockchain.home.presentation.handhold.HandholdViewModel
import com.blockchain.home.presentation.handhold.HandholdViewState
import com.blockchain.home.presentation.navigation.ARG_ACTIVITY_TX_ID
import com.blockchain.home.presentation.navigation.ARG_FIAT_TICKER
import com.blockchain.home.presentation.navigation.ARG_QUICK_ACTION_VM_KEY
import com.blockchain.home.presentation.navigation.ARG_RECURRING_BUY_ID
import com.blockchain.home.presentation.navigation.ARG_WALLET_MODE
import com.blockchain.home.presentation.navigation.HomeDestination
import com.blockchain.home.presentation.news.NewsIntent
import com.blockchain.home.presentation.news.NewsViewModel
import com.blockchain.home.presentation.news.NewsViewState
import com.blockchain.home.presentation.quickactions.QuickActions
import com.blockchain.home.presentation.quickactions.QuickActionsIntent
import com.blockchain.home.presentation.quickactions.QuickActionsNavEvent
import com.blockchain.home.presentation.quickactions.QuickActionsViewModel
import com.blockchain.home.presentation.quickactions.QuickActionsViewState
import com.blockchain.home.presentation.quickactions.maxQuickActionsOnScreen
import com.blockchain.home.presentation.recurringbuy.list.RecurringBuyEligibleState
import com.blockchain.home.presentation.recurringbuy.list.RecurringBuysIntent
import com.blockchain.home.presentation.recurringbuy.list.RecurringBuysViewModel
import com.blockchain.home.presentation.recurringbuy.list.RecurringBuysViewState
import com.blockchain.koin.payloadScope
import com.blockchain.presentation.urllinks.URL_KYC_REJETED_SUPPORT
import com.blockchain.prices.prices.PricesIntents
import com.blockchain.prices.prices.PricesLoadStrategy
import com.blockchain.prices.prices.PricesViewModel
import com.blockchain.prices.prices.PricesViewState
import com.blockchain.prices.prices.percentAndPositionOf
import com.blockchain.stringResources.R
import com.blockchain.transactions.receive.navigation.ReceiveDestination
import com.blockchain.walletconnect.domain.WalletConnectAnalytics
import com.blockchain.walletconnect.ui.composable.common.DappSessionUiElement
import com.blockchain.walletconnect.ui.navigation.WalletConnectDestination
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetInfo
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@Composable
fun HomeScreen(
    analytics: Analytics = get(),
    listState: LazyListState,
    isSwipingToRefresh: Boolean,
    openSettings: () -> Unit,
    launchQrScanner: () -> Unit,
    startPhraseRecovery: () -> Unit,
    processAnnouncementUrl: (String) -> Unit,
    navigateToMode: (WalletMode) -> Unit,
) {
    val navController = LocalNavControllerProvider.current
    val assetActionsNavigation = LocalAssetActionsNavigationProvider.current
    val recurringBuyNavigation = LocalRecurringBuyNavigationProvider.current
    val supportNavigation = LocalSupportNavigationProvider.current

    RuntimePermission(permission = RuntimePermission.Notification)

    // -----------------------------
    // header scroll animation
    var menuOptionsHeight: Int by remember { mutableIntStateOf(0) }
    var balanceOffsetToMenuOption: Float by remember { mutableFloatStateOf(0F) }
    val balanceToMenuPaddingPx: Int = LocalDensity.current.run { 24.dp.toPx() }.toInt()
    var balanceScrollRange: Float by remember { mutableFloatStateOf(0F) }

    val showMenuOptionsBackground: Boolean = balanceOffsetToMenuOption <= 0F && menuOptionsHeight > 0F
    val showMenuOptionsBalance = balanceScrollRange <= 0.5 && menuOptionsHeight > 0F
    val hideBalance = balanceScrollRange <= 0.5 && menuOptionsHeight > 0F
    fun menuOptionsHeightLoaded(height: Int) {
        if (menuOptionsHeight == 0) menuOptionsHeight = height
    }

    fun balanceYPositionLoaded(balanceYPosition: Float) {
        (balanceYPosition - menuOptionsHeight + balanceToMenuPaddingPx)
            .coerceAtLeast(0F).let {
                if (balanceOffsetToMenuOption != it) balanceOffsetToMenuOption = it
            }

        ((balanceYPosition / menuOptionsHeight.toFloat()) * 2).coerceIn(0F, 1F).let {
            if (balanceScrollRange != it) balanceScrollRange = it
        }
    }

    // -----------------------------
    // navigation
    fun openFailedBalancesInfo() {
        navController.navigate(HomeDestination.FailedBalances)
    }

    fun openReceive() {
        navController.navigate(ReceiveDestination.Accounts)
    }

    fun openAssetsList(assetsCount: Int) {
        navController.navigate(HomeDestination.CryptoAssets)
        analytics.logEvent(
            DashboardAnalyticsEvents.AssetsSeeAllClicked(assetsCount = assetsCount)
        )
    }

    fun openCoinview(asset: AssetInfo) {
        assetActionsNavigation.coinview(asset)
        analytics.logEvent(
            DashboardAnalyticsEvents.CryptoAssetClicked(ticker = asset.displayTicker)
        )
    }

    fun openActivityList() {
        navController.navigate(HomeDestination.Activity)
        analytics.logEvent(DashboardAnalyticsEvents.ActivitySeeAllClicked)
    }

    fun openActivityDetail(txId: String, walletMode: WalletMode) {
        navController.navigate(
            destination = HomeDestination.ActivityDetail,
            args = listOf(
                NavArgument(key = ARG_ACTIVITY_TX_ID, value = txId),
                NavArgument(key = ARG_WALLET_MODE, value = walletMode)
            )
        )
    }

    fun openRecurringBuysList() {
        navController.navigate(HomeDestination.RecurringBuys)
    }

    fun openRecurringBuyDetail(id: String) {
        navController.navigate(
            destination = HomeDestination.RecurringBuyDetail,
            args = listOf(
                NavArgument(key = ARG_RECURRING_BUY_ID, value = id)
            )
        )
    }

    fun openRecurringBuyOnboarding() {
        recurringBuyNavigation.openOnboarding()
    }

    fun openSupportCenter() {
        supportNavigation.launchSupportCenter()
    }

    fun openSwapDexOptions() {
        navController.navigate(HomeDestination.SwapDexOptions)
    }

    fun openMoreQuickActions(walletMode: WalletMode) {
        navController.navigate(
            HomeDestination.MoreQuickActions,
            listOf(
                NavArgument(key = ARG_QUICK_ACTION_VM_KEY, value = walletMode.name + "qa"),
            )
        )
    }

    fun openFiatActionDetail(fiatTicker: String) {
        navController.navigate(
            HomeDestination.FiatActionDetail,
            listOf(NavArgument(key = ARG_FIAT_TICKER, fiatTicker))
        )
        analytics.logEvent(DashboardAnalyticsEvents.FiatAssetClicked(ticker = fiatTicker))
    }

    fun openFundsLockDetail(fundsLocks: FundsLocks) {
        assetActionsNavigation.fundsLocksDetail(fundsLocks)
    }

    fun openWalletConnectManageSession(dappSessionUiElement: DappSessionUiElement) {
        analytics.logEvent(WalletConnectAnalytics.HomeDappClicked(dappSessionUiElement.chainName))

        navController.navigate(
            WalletConnectDestination.WalletConnectManageSession,
            listOfNotNull(
                NavArgument(key = WalletConnectDestination.ARG_SESSION_ID, value = dappSessionUiElement.sessionId),
                NavArgument(key = WalletConnectDestination.ARG_IS_V2_SESSION, value = dappSessionUiElement.isV2)
            )
        )
    }

    fun openWalletConnectDappList() {
        analytics.logEvent(WalletConnectAnalytics.ConnectedDappsListClicked(origin = LaunchOrigin.HOME))

        navController.navigate(WalletConnectDestination.WalletConnectDappList)
    }
    //

    val walletMode by get<WalletModeService>(scope = payloadScope).walletMode.collectAsStateLifecycleAware(null)

    LaunchedEffect(walletMode) {
        walletMode?.let {
            analytics.logEvent(DashboardAnalyticsEvents.ModeViewed(walletMode = it))
        }
    }

    walletMode?.let {
        when (it) {
            WalletMode.CUSTODIAL -> {
                CustodialHomeDashboard(
                    analytics = analytics,

                    isSwipingToRefresh = isSwipingToRefresh,

                    listState = listState,
                    openSettings = openSettings,
                    launchQrScanner = launchQrScanner,

                    goToDefi = { navigateToMode(WalletMode.NON_CUSTODIAL) },
                    processAnnouncementUrl = processAnnouncementUrl,
                    startPhraseRecovery = startPhraseRecovery,

                    assetActionsNavigation = assetActionsNavigation,
                    openMoreQuickActions = { openMoreQuickActions(WalletMode.CUSTODIAL) },
                    openReceive = ::openReceive,

                    openCryptoAssets = ::openAssetsList,
                    assetOnClick = ::openCoinview,
                    fundsLocksOnClick = ::openFundsLockDetail,
                    openFiatActionDetail = ::openFiatActionDetail,

                    openRecurringBuys = ::openRecurringBuysList,
                    upsellOnClick = ::openRecurringBuyOnboarding,
                    recurringBuyOnClick = ::openRecurringBuyDetail,

                    openActivity = ::openActivityList,
                    openActivityDetail = ::openActivityDetail,

                    openSupportCenter = ::openSupportCenter,

                    showMenuOptionsBackground = showMenuOptionsBackground,
                    showMenuOptionsBalance = showMenuOptionsBalance,
                    balanceAlphaProvider = { balanceScrollRange },
                    hideBalance = hideBalance,
                    menuOptionsHeightLoaded = ::menuOptionsHeightLoaded,
                    balanceYPositionLoaded = ::balanceYPositionLoaded
                )
            }

            WalletMode.NON_CUSTODIAL -> {
                DefiHomeDashboard(
                    analytics = analytics,

                    isSwipingToRefresh = isSwipingToRefresh,

                    listState = listState,
                    openSettings = openSettings,
                    launchQrScanner = launchQrScanner,

                    processAnnouncementUrl = processAnnouncementUrl,
                    startPhraseRecovery = startPhraseRecovery,

                    assetActionsNavigation = assetActionsNavigation,
                    openDexSwapOptions = ::openSwapDexOptions,
                    openMoreQuickActions = { openMoreQuickActions(WalletMode.NON_CUSTODIAL) },
                    openReceive = ::openReceive,

                    openFailedBalancesInfo = ::openFailedBalancesInfo,

                    openCryptoAssets = ::openAssetsList,
                    assetOnClick = ::openCoinview,

                    onDappSessionClicked = ::openWalletConnectManageSession,
                    onWalletConnectSeeAllSessionsClicked = ::openWalletConnectDappList,

                    openActivity = ::openActivityList,
                    openActivityDetail = ::openActivityDetail,

                    openSupportCenter = ::openSupportCenter,

                    showMenuOptionsBackground = showMenuOptionsBackground,
                    showMenuOptionsBalance = showMenuOptionsBalance,
                    balanceAlphaProvider = { balanceScrollRange },
                    hideBalance = hideBalance,
                    menuOptionsHeightLoaded = ::menuOptionsHeightLoaded,
                    balanceYPositionLoaded = ::balanceYPositionLoaded
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustodialHomeDashboard(
    analytics: Analytics,

    isSwipingToRefresh: Boolean,

    listState: LazyListState,
    openSettings: () -> Unit,
    launchQrScanner: () -> Unit,

    goToDefi: () -> Unit,

    processAnnouncementUrl: (String) -> Unit,
    startPhraseRecovery: () -> Unit,

    assetActionsNavigation: AssetActionsNavigation,
    openMoreQuickActions: () -> Unit,
    openReceive: () -> Unit,

    assetOnClick: (AssetInfo) -> Unit,
    openCryptoAssets: (count: Int) -> Unit,
    fundsLocksOnClick: (FundsLocks) -> Unit,
    openFiatActionDetail: (String) -> Unit,

    openRecurringBuys: () -> Unit,
    upsellOnClick: () -> Unit,
    recurringBuyOnClick: (String) -> Unit,

    openActivity: () -> Unit,
    openActivityDetail: (String, WalletMode) -> Unit,

    openSupportCenter: () -> Unit,

    showMenuOptionsBackground: Boolean = false,
    showMenuOptionsBalance: Boolean = false,
    balanceAlphaProvider: () -> Float,
    hideBalance: Boolean,
    menuOptionsHeightLoaded: (Int) -> Unit,
    balanceYPositionLoaded: (Float) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = LocalNavControllerProvider.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val handholdViewModel: HandholdViewModel = getViewModel(scope = payloadScope)
    val handholdViewState: HandholdViewState by handholdViewModel.viewState.collectAsStateLifecycleAware()
    val quickActionsViewModel: QuickActionsViewModel = getViewModel(
        scope = payloadScope,
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner,
        key = WalletMode.CUSTODIAL.name + "qa"
    )
    val quickActionsState: QuickActionsViewState by quickActionsViewModel.viewState.collectAsStateLifecycleAware()
    val maxQuickActions = maxQuickActionsOnScreen
    val announcementsViewModel: AnnouncementsViewModel = getViewModel(scope = payloadScope)
    val announcementsState: AnnouncementsViewState by announcementsViewModel.viewState.collectAsStateLifecycleAware()
    val homeAssetsViewModel: AssetsViewModel = getViewModel(
        scope = payloadScope, key = WalletMode.CUSTODIAL.name + "assets"
    )
    val assetsViewState: AssetsViewState by homeAssetsViewModel.viewState.collectAsStateLifecycleAware()
    val rbViewModel: RecurringBuysViewModel = getViewModel(scope = payloadScope)
    val rbViewState: RecurringBuysViewState by rbViewModel.viewState.collectAsStateLifecycleAware()
    val pricesViewModel: PricesViewModel = getViewModel(
        scope = payloadScope, key = WalletMode.CUSTODIAL.name + "prices"
    )
    val pricesViewState: PricesViewState by pricesViewModel.viewState.collectAsStateLifecycleAware()
    val activityViewModel: CustodialActivityViewModel = getViewModel(scope = payloadScope)
    val activityViewState: ActivityViewState by activityViewModel.viewState.collectAsStateLifecycleAware()
    val newsViewModel: NewsViewModel = getViewModel(
        scope = payloadScope, key = WalletMode.CUSTODIAL.name + "news"
    )
    val newsViewState: NewsViewState by newsViewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // onboarding handhold
                handholdViewModel.onIntent(HandholdIntent.LoadData)
                // quick action
                quickActionsViewModel.onIntent(
                    QuickActionsIntent.LoadActions(
                        walletMode = WalletMode.CUSTODIAL,
                        maxQuickActionsOnScreen = maxQuickActions
                    )
                )
                // announcements
                announcementsViewModel.onIntent(
                    AnnouncementsIntent.LoadAnnouncements(
                        walletMode = WalletMode.CUSTODIAL,
                    )
                )
                // accounts
                homeAssetsViewModel.onIntent(AssetsIntent.LoadFilters)
                homeAssetsViewModel.onIntent(
                    AssetsIntent.LoadAccounts(
                        walletMode = WalletMode.CUSTODIAL,
                        sectionSize = SectionSize.Limited(MAX_ASSET_COUNT)
                    )
                )
                homeAssetsViewModel.onIntent(AssetsIntent.LoadFundLocks)
                // rb
                rbViewModel.onIntent(RecurringBuysIntent.LoadRecurringBuys(SectionSize.Limited(MAX_RB_COUNT)))
                // top movers
                pricesViewModel.onIntent(PricesIntents.LoadData(PricesLoadStrategy.TradableOnly))
                // activity
                activityViewModel.onIntent(ActivityIntent.LoadActivity(SectionSize.Limited(MAX_ACTIVITY_COUNT)))
                // news
                newsViewModel.onIntent(NewsIntent.LoadData(walletMode = WalletMode.CUSTODIAL))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isSwipingToRefresh) {
        if (isSwipingToRefresh) {
            announcementsViewModel.onIntent(AnnouncementsIntent.Refresh)
            homeAssetsViewModel.onIntent(AssetsIntent.Refresh)
            pricesViewModel.onIntent(PricesIntents.Refresh)
            quickActionsViewModel.onIntent(QuickActionsIntent.Refresh)
            activityViewModel.onIntent(ActivityIntent.Refresh())
            newsViewModel.onIntent(NewsIntent.Refresh)
        }
    }

    // quick actions navigation
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_CREATE) {
                scope.launch {
                    quickActionsViewModel.navigationEventFlow.collectLatest {
                        when (it) {
                            QuickActionsNavEvent.Send -> assetActionsNavigation.navigate(AssetAction.Send)
                            QuickActionsNavEvent.Sell -> assetActionsNavigation.navigate(AssetAction.Sell)
                            QuickActionsNavEvent.Receive -> openReceive()
                            QuickActionsNavEvent.Buy -> assetActionsNavigation.navigate(AssetAction.Buy)
                            QuickActionsNavEvent.Swap -> assetActionsNavigation.navigate(AssetAction.Swap)

                            QuickActionsNavEvent.FiatWithdraw -> quickActionsViewModel.onIntent(
                                QuickActionsIntent.FiatAction(AssetAction.FiatWithdraw)
                            )

                            QuickActionsNavEvent.More -> openMoreQuickActions()
                            QuickActionsNavEvent.DexOrSwapOption -> {} // n/a
                            QuickActionsNavEvent.KycVerificationPrompt ->
                                navController.navigate(HomeDestination.KycVerificationPrompt)
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val balance = (assetsViewState.balance.balance as? DataResource.Data)?.data
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(
                AppTheme.colors.background
            )
    ) {
        stickyHeader {
            MenuOptionsScreen(
                modifier = Modifier.onGloballyPositioned {
                    menuOptionsHeightLoaded(it.size.height)
                },
                walletBalanceCurrency = balance?.symbol.orEmpty(),
                walletBalance = balance?.toStringWithoutSymbol().orEmpty(),
                openSettings = openSettings,
                launchQrScanner = launchQrScanner,
                showBackground = showMenuOptionsBackground,
                showBalance = showMenuOptionsBalance
            )
        }

        item {
            BalanceScreen(
                modifier = Modifier.onGloballyPositioned {
                    balanceYPositionLoaded(it.positionInParent().y)
                },
                balanceAlphaProvider = balanceAlphaProvider,
                hideBalance = hideBalance,
                walletBalance = assetsViewState.balance,
            )
        }

        // quick actions
        // should not be shown if kyc is rejected
        if (!handholdViewState.showKycRejected) {
            paddedItem(
                paddingValues = {
                    PaddingValues(AppTheme.dimensions.smallSpacing)
                }
            ) {
                QuickActions(
                    quickActionItems = quickActionsState.actions.toImmutableList(),
                    dashboardState = dashboardState(assetsViewState),
                    quickActionClicked = {
                        quickActionsViewModel.onIntent(QuickActionsIntent.ActionClicked(it))
                    }
                )
            }
        }

        // announcements, assets, dapps, rb, top movers, activity, news
        // should wait for handhold status
        handholdViewState.showHandhold.doOnData { showHandhold ->
            when (showHandhold) {
                true -> {
                    // at this point the dashboard will only be handhold + help
                    val handholdTasks = (handholdViewState.tasksStatus as DataResource.Data).data.toImmutableList()
                    handhold(
                        data = handholdTasks,
                        onClick = {
                            when (it) {
                                HandholdTask.VerifyEmail -> {
                                    navController.navigate(HomeDestination.EmailVerification)
                                }

                                HandholdTask.Kyc -> {
                                    assetActionsNavigation.startKyc()
                                }

                                HandholdTask.BuyCrypto -> {
                                    assetActionsNavigation.navigate(AssetAction.Buy)
                                }
                            }
                        }
                    )

                    homeHelp(
                        openSupportCenter = openSupportCenter
                    )
                }

                false -> {
                    // if user has no balance and kyc is rejected -> block custodial wallet
                    // should be shown if kyc rejected && balance == 0
                    val showBlockingKycCard = handholdViewState.showKycRejected &&
                        (balance?.isZero ?: false)

                    when (showBlockingKycCard) {
                        true -> {
                            // blocked ⛔️
                            kycRejected(
                                onClick = goToDefi
                            )
                        }

                        false -> {
                            // unlocked/full dashboard here ✅

                            // kyc rejected warning, when user has balance > 0
                            // should be shown if kyc is rejected && balance > 0
                            val showKycRejectedWithBalance = handholdViewState.showKycRejected &&
                                (balance?.isPositive ?: false)

                            if (showKycRejectedWithBalance) {
                                paddedItem(
                                    paddingValues = {
                                        PaddingValues(AppTheme.dimensions.smallSpacing)
                                    }
                                ) {
                                    CardAlert(
                                        title = stringResource(R.string.dashboard_kyc_rejected_with_balance_title),
                                        subtitle = stringResource(
                                            R.string.dashboard_kyc_rejected_with_balance_description
                                        ),
                                        isDismissable = false,
                                        alertType = AlertType.Warning,
                                        primaryCta = CardButton(
                                            text = stringResource(R.string.dashboard_kyc_rejected_with_balance_support),
                                            onClick = {
                                                context.openUrl(URL_KYC_REJETED_SUPPORT)
                                            }
                                        )
                                    )
                                }
                            }

                            // announcements
                            item {
                                (announcementsState.remoteAnnouncements as? DataResource.Data)?.data?.let {
                                    StackedAnnouncements(
                                        announcements = it,
                                        hideConfirmation = announcementsState.hideAnnouncementsConfirmation,
                                        animateHideConfirmation = announcementsState
                                            .animateHideAnnouncementsConfirmation,
                                        announcementOnSwiped = { announcement ->
                                            announcementsViewModel.onIntent(
                                                AnnouncementsIntent.DeleteAnnouncement(announcement)
                                            )
                                        },
                                        announcementOnClick = { announcement ->
                                            processAnnouncementUrl(announcement.actionUrl)
                                            announcementsViewModel.onIntent(
                                                AnnouncementsIntent.AnnouncementClicked(announcement)
                                            )
                                        }
                                    )
                                }
                            }

                            announcementsState.localAnnouncements.takeIf { it.isNotEmpty() }
                                ?.let { localAnnouncements ->
                                    paddedItem(
                                        paddingValues = { PaddingValues(AppTheme.dimensions.smallSpacing) }
                                    ) {
                                        LocalAnnouncements(
                                            announcements = localAnnouncements,
                                            onClick = { announcement ->
                                                when (announcement.type) {
                                                    LocalAnnouncementType.PHRASE_RECOVERY -> startPhraseRecovery()
                                                }
                                            }
                                        )
                                    }
                                }

                            // assets
                            val assets = (assetsViewState.assets as? DataResource.Data)?.data
                            val locks = (assetsViewState.fundsLocks as? DataResource.Data)?.data

                            assets?.takeIf { it.isNotEmpty() }?.let { data ->
                                homeAssets(
                                    locks = locks,
                                    data = assets,
                                    openCryptoAssets = { openCryptoAssets(data.size) },
                                    assetOnClick = assetOnClick,
                                    fundsLocksOnClick = fundsLocksOnClick,
                                    openFiatActionDetail = openFiatActionDetail
                                )
                            }

                            // rb
                            rbViewState.recurringBuys
                                .map { state ->
                                    (state as? RecurringBuyEligibleState.Eligible)?.recurringBuys
                                }
                                .dataOrElse(null)
                                ?.let { recurringBuys ->
                                    homeRecurringBuys(
                                        analytics = analytics,
                                        recurringBuys = recurringBuys.toImmutableList(),
                                        manageOnclick = openRecurringBuys,
                                        upsellOnClick = upsellOnClick,
                                        recurringBuyOnClick = recurringBuyOnClick
                                    )
                                }

                            // top movers
                            homeTopMovers(
                                data = pricesViewState.topMovers.toImmutableList(),
                                assetOnClick = { asset ->
                                    assetActionsNavigation.coinview(asset)

                                    pricesViewState.topMovers.percentAndPositionOf(asset)
                                        ?.let { (percentageMove, position) ->
                                            analytics.logEvent(
                                                DashboardAnalyticsEvents.TopMoverAssetClicked(
                                                    ticker = asset.networkTicker,
                                                    percentageMove = percentageMove,
                                                    position = position
                                                )
                                            )
                                        }
                                }
                            )

                            // activity
                            homeActivityScreen(
                                activityState = activityViewState,
                                openActivity = openActivity,
                                openActivityDetail = openActivityDetail,
                                wMode = WalletMode.CUSTODIAL
                            )

                            // news
                            homeNews(
                                data = newsViewState.newsArticles?.toImmutableList(),
                                seeAllOnClick = {
                                    navController.navigate(HomeDestination.News)
                                }
                            )

                            // help
                            homeHelp(
                                openSupportCenter = openSupportCenter
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.borderRadiiLarge))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DefiHomeDashboard(
    analytics: Analytics,

    isSwipingToRefresh: Boolean,

    listState: LazyListState,
    openSettings: () -> Unit,
    launchQrScanner: () -> Unit,

    processAnnouncementUrl: (String) -> Unit,
    startPhraseRecovery: () -> Unit,

    assetActionsNavigation: AssetActionsNavigation,
    openDexSwapOptions: () -> Unit,
    openMoreQuickActions: () -> Unit,
    openReceive: () -> Unit,

    openFailedBalancesInfo: () -> Unit,

    assetOnClick: (AssetInfo) -> Unit,
    openCryptoAssets: (count: Int) -> Unit,

    onDappSessionClicked: (DappSessionUiElement) -> Unit,
    onWalletConnectSeeAllSessionsClicked: () -> Unit,

    openActivity: () -> Unit,
    openActivityDetail: (String, WalletMode) -> Unit,

    openSupportCenter: () -> Unit,

    showMenuOptionsBackground: Boolean,
    showMenuOptionsBalance: Boolean,
    balanceAlphaProvider: () -> Float,
    hideBalance: Boolean,
    menuOptionsHeightLoaded: (Int) -> Unit,
    balanceYPositionLoaded: (Float) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = LocalNavControllerProvider.current

    val quickActionsViewModel: QuickActionsViewModel = getViewModel(
        scope = payloadScope,
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner,
        key = WalletMode.NON_CUSTODIAL.name + "qa"
    )
    val quickActionsState: QuickActionsViewState by quickActionsViewModel.viewState.collectAsStateLifecycleAware()
    val maxQuickActions = maxQuickActionsOnScreen
    val announcementsViewModel: AnnouncementsViewModel = getViewModel(scope = payloadScope)
    val announcementsState: AnnouncementsViewState by announcementsViewModel.viewState.collectAsStateLifecycleAware()
    val failedBalancesViewModel: FailedBalancesViewModel = getViewModel(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner, scope = payloadScope
    )
    val failedBalancesViewState: FailedBalancesViewState by failedBalancesViewModel.viewState
        .collectAsStateLifecycleAware()
    val homeAssetsViewModel: AssetsViewModel = getViewModel(
        scope = payloadScope, key = WalletMode.NON_CUSTODIAL.name + "assets"
    )
    val assetsViewState: AssetsViewState by homeAssetsViewModel.viewState.collectAsStateLifecycleAware()
    val homeDappsViewModel: HomeDappsViewModel = getViewModel(scope = payloadScope)
    val homeDappsState: HomeDappsViewState by homeDappsViewModel.viewState.collectAsStateLifecycleAware()
    val pricesViewModel: PricesViewModel = getViewModel(
        scope = payloadScope, key = WalletMode.NON_CUSTODIAL.name + "prices"
    )
    val pricesViewState: PricesViewState by pricesViewModel.viewState.collectAsStateLifecycleAware()
    val activityViewModel: PrivateKeyActivityViewModel = getViewModel(scope = payloadScope)
    val activityViewState: ActivityViewState by activityViewModel.viewState.collectAsStateLifecycleAware()
    val newsViewModel: NewsViewModel = getViewModel(
        scope = payloadScope, key = WalletMode.NON_CUSTODIAL.name + "news"
    )
    val newsViewState: NewsViewState by newsViewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_CREATE) {
                activityViewModel.onIntent(ActivityIntent.LoadActivity(SectionSize.Limited(MAX_ACTIVITY_COUNT)))
            }
            if (event == Lifecycle.Event.ON_RESUME) {
                // quick action
                quickActionsViewModel.onIntent(
                    QuickActionsIntent.LoadActions(
                        walletMode = WalletMode.NON_CUSTODIAL,
                        maxQuickActionsOnScreen = maxQuickActions
                    )
                )
                // announcements
                announcementsViewModel.onIntent(
                    AnnouncementsIntent.LoadAnnouncements(
                        walletMode = WalletMode.CUSTODIAL,
                    )
                )
                // failed balances
                failedBalancesViewModel.onIntent(FailedBalancesIntent.LoadData)
                // accounts
                homeAssetsViewModel.onIntent(AssetsIntent.LoadFilters)
                homeAssetsViewModel.onIntent(
                    AssetsIntent.LoadAccounts(
                        walletMode = WalletMode.NON_CUSTODIAL,
                        sectionSize = SectionSize.Limited(MAX_ASSET_COUNT)
                    )
                )
                // dapps
                homeDappsViewModel.onIntent(HomeDappsIntent.LoadData)
                // top movers
                pricesViewModel.onIntent(PricesIntents.LoadData(PricesLoadStrategy.All))
                // news
                newsViewModel.onIntent(NewsIntent.LoadData(walletMode = WalletMode.NON_CUSTODIAL))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isSwipingToRefresh) {
        if (isSwipingToRefresh) {
            announcementsViewModel.onIntent(AnnouncementsIntent.Refresh)
            failedBalancesViewModel.onIntent(FailedBalancesIntent.Refresh)
            homeAssetsViewModel.onIntent(AssetsIntent.Refresh)
            quickActionsViewModel.onIntent(QuickActionsIntent.Refresh)
            pricesViewModel.onIntent(PricesIntents.Refresh)
            activityViewModel.onIntent(ActivityIntent.Refresh())
            newsViewModel.onIntent(NewsIntent.Refresh)
        }
    }
    val scope = rememberCoroutineScope()
    // quick actions navigation
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_CREATE) {
                scope.launch {
                    quickActionsViewModel.navigationEventFlow.collectLatest {
                        when (it) {
                            QuickActionsNavEvent.Send -> assetActionsNavigation.navigate(AssetAction.Send)
                            QuickActionsNavEvent.Sell -> assetActionsNavigation.navigate(AssetAction.Sell)
                            QuickActionsNavEvent.Receive -> openReceive()
                            QuickActionsNavEvent.Swap -> assetActionsNavigation.navigate(AssetAction.Swap)
                            QuickActionsNavEvent.DexOrSwapOption -> openDexSwapOptions()
                            QuickActionsNavEvent.More -> openMoreQuickActions()
                            QuickActionsNavEvent.KycVerificationPrompt ->
                                navController.navigate(HomeDestination.KycVerificationPrompt)
                            QuickActionsNavEvent.Buy,
                            QuickActionsNavEvent.FiatWithdraw -> {
                            } // n/a
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val balance = (assetsViewState.balance.balance as? DataResource.Data)?.data
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(
                AppTheme.colors.background
            )
    ) {
        stickyHeader {
            MenuOptionsScreen(
                modifier = Modifier.onGloballyPositioned {
                    menuOptionsHeightLoaded(it.size.height)
                },
                walletBalanceCurrency = balance?.symbol.orEmpty(),
                walletBalance = balance?.toStringWithoutSymbol().orEmpty(),
                openSettings = openSettings,
                launchQrScanner = launchQrScanner,
                showBackground = showMenuOptionsBackground,
                showBalance = showMenuOptionsBalance
            )
        }

        item {
            BalanceScreen(
                modifier = Modifier.onGloballyPositioned {
                    balanceYPositionLoaded(it.positionInParent().y)
                },
                balanceAlphaProvider = balanceAlphaProvider,
                hideBalance = hideBalance,
                walletBalance = assetsViewState.balance,
            )
        }

        paddedItem(
            paddingValues = {
                PaddingValues(AppTheme.dimensions.smallSpacing)
            }
        ) {
            QuickActions(
                quickActionItems = quickActionsState.actions.toImmutableList(),
                dashboardState = dashboardState(assetsViewState),
                quickActionClicked = {
                    quickActionsViewModel.onIntent(QuickActionsIntent.ActionClicked(it))
                }
            )
        }

        // anouncements
        item {
            (announcementsState.remoteAnnouncements as? DataResource.Data)?.data?.let { announcements ->
                StackedAnnouncements(
                    announcements = announcements,
                    hideConfirmation = announcementsState.hideAnnouncementsConfirmation,
                    animateHideConfirmation = announcementsState.animateHideAnnouncementsConfirmation,
                    announcementOnSwiped = { announcement ->
                        announcementsViewModel.onIntent(
                            AnnouncementsIntent.DeleteAnnouncement(announcement)
                        )
                    },
                    announcementOnClick = { announcement ->
                        processAnnouncementUrl(announcement.actionUrl)
                        announcementsViewModel.onIntent(
                            AnnouncementsIntent.AnnouncementClicked(announcement)
                        )
                    }
                )
            }
        }

        announcementsState.localAnnouncements.takeIf { it.isNotEmpty() }
            ?.let { localAnnouncements ->
                paddedItem(
                    paddingValues = { PaddingValues(AppTheme.dimensions.smallSpacing) }
                ) {
                    LocalAnnouncements(
                        announcements = localAnnouncements,
                        onClick = { announcement ->
                            when (announcement.type) {
                                LocalAnnouncementType.PHRASE_RECOVERY -> startPhraseRecovery()
                            }
                        }
                    )
                }
            }

        // failed network balances
        val failedNetworkNames = (failedBalancesViewState.failedNetworkNames as? DataResource.Data)?.data
        if (!failedBalancesViewState.dismissWarning) {
            homeFailedBalances(
                failedNetworkNames = failedNetworkNames?.toImmutableList(),
                dismissFailedNetworksWarning = {
                    failedBalancesViewModel.onIntent(FailedBalancesIntent.DismissFailedNetworksWarning)
                },
                learnMoreOnClick = openFailedBalancesInfo
            )
        }

        // assets
        val assets = (assetsViewState.assets as? DataResource.Data)?.data
        assets?.let {
            if (assets.isNotEmpty()) {
                homeAssets(
                    locks = null,
                    data = assets,
                    openCryptoAssets = { openCryptoAssets(assets.size) },
                    assetOnClick = assetOnClick,
                    fundsLocksOnClick = {}, // n/a nc
                    openFiatActionDetail = {}, // n/a nc
                    showWarning = failedNetworkNames?.isNotEmpty() ?: false,
                    warningOnClick = openFailedBalancesInfo
                )
            } else {
                defiEmptyCard(
                    assetsViewState = assetsViewState,
                    onReceiveClicked = openReceive
                )
            }
        }

        balance?.let { balance ->
            if (balance.isPositive) {
                // dapps
                homeDapps(
                    homeDappsState = homeDappsState,
                    openQrCodeScanner = launchQrScanner,
                    onDappSessionClicked = onDappSessionClicked,
                    onWalletConnectSeeAllSessionsClicked = onWalletConnectSeeAllSessionsClicked,
                )
            }
        }

        // top movers
        homeTopMovers(
            data = pricesViewState.topMovers.toImmutableList(),
            assetOnClick = { asset ->
                assetActionsNavigation.coinview(asset)

                pricesViewState.topMovers.percentAndPositionOf(asset)
                    ?.let { (percentageMove, position) ->
                        analytics.logEvent(
                            DashboardAnalyticsEvents.TopMoverAssetClicked(
                                ticker = asset.networkTicker,
                                percentageMove = percentageMove,
                                position = position
                            )
                        )
                    }
            }
        )

        // activity
        homeActivityScreen(
            activityState = activityViewState,
            openActivity = openActivity,
            openActivityDetail = openActivityDetail,
            wMode = WalletMode.NON_CUSTODIAL,
            showWarning = failedNetworkNames?.isNotEmpty() ?: false,
            warningOnClick = openFailedBalancesInfo
        )

        // news
        homeNews(
            data = newsViewState.newsArticles?.toImmutableList(),
            seeAllOnClick = {
                navController.navigate(HomeDestination.News)
            }
        )

        // help
        homeHelp(
            openSupportCenter = openSupportCenter
        )

        item {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.borderRadiiLarge))
        }
    }
}

private const val MAX_ASSET_COUNT = 7
private const val MAX_ACTIVITY_COUNT = 5
private const val MAX_RB_COUNT = 5
