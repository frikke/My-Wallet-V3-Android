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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelStoreOwner
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.chrome.MenuOptionsScreen
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.data.dataOrElse
import com.blockchain.data.map
import com.blockchain.data.toImmutableList
import com.blockchain.domain.referral.model.ReferralInfo
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
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.home.presentation.navigation.RecurringBuyNavigation
import com.blockchain.home.presentation.navigation.SupportNavigation
import com.blockchain.home.presentation.news.NewsViewModel
import com.blockchain.home.presentation.news.NewsViewState
import com.blockchain.home.presentation.quickactions.QuickActions
import com.blockchain.home.presentation.quickactions.QuickActionsIntent
import com.blockchain.home.presentation.quickactions.QuickActionsViewModel
import com.blockchain.home.presentation.quickactions.QuickActionsViewState
import com.blockchain.home.presentation.quickactions.maxQuickActionsOnScreen
import com.blockchain.home.presentation.recurringbuy.list.RecurringBuyEligibleState
import com.blockchain.home.presentation.recurringbuy.list.RecurringBuysIntent
import com.blockchain.home.presentation.recurringbuy.list.RecurringBuysViewModel
import com.blockchain.home.presentation.recurringbuy.list.RecurringBuysViewState
import com.blockchain.home.presentation.referral.ReferralIntent
import com.blockchain.home.presentation.referral.ReferralViewModel
import com.blockchain.home.presentation.referral.ReferralViewState
import com.blockchain.koin.payloadScope
import com.blockchain.prices.prices.PricesIntents
import com.blockchain.prices.prices.PricesLoadStrategy
import com.blockchain.prices.prices.PricesViewModel
import com.blockchain.prices.prices.PricesViewState
import com.blockchain.prices.prices.percentAndPositionOf
import com.blockchain.walletconnect.ui.composable.common.DappSessionUiElement
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    analytics: Analytics = get(),
    listState: LazyListState,
    isSwipingToRefresh: Boolean,
    assetActionsNavigation: AssetActionsNavigation,
    recurringBuyNavigation: RecurringBuyNavigation,
    supportNavigation: SupportNavigation,
    openSettings: () -> Unit,
    launchQrScanner: () -> Unit,
    openCryptoAssets: () -> Unit,
    openRecurringBuys: () -> Unit,
    openRecurringBuyDetail: (String) -> Unit,
    openActivity: () -> Unit,
    openActivityDetail: (String, WalletMode) -> Unit,
    openReferral: () -> Unit,
    openSwapDexOption: () -> Unit,
    openFiatActionDetail: (String) -> Unit,
    openMoreQuickActions: () -> Unit,
    startPhraseRecovery: () -> Unit,
    processAnnouncementUrl: (String) -> Unit,
    openSwap: () -> Unit,
    onWalletConnectSessionClicked: (DappSessionUiElement) -> Unit,
    onWalletConnectSeeAllSessionsClicked: () -> Unit,
) {
    var menuOptionsHeight: Int by remember { mutableStateOf(0) }
    var balanceOffsetToMenuOption: Float by remember { mutableStateOf(0F) }
    val balanceToMenuPaddingPx: Int = LocalDensity.current.run { 24.dp.toPx() }.toInt()
    var balanceScrollRange: Float by remember { mutableStateOf(0F) }

    val lifecycleOwner = LocalLifecycleOwner.current

    val homeAssetsViewModel: AssetsViewModel = getViewModel(scope = payloadScope)
    val assetsViewState: AssetsViewState by homeAssetsViewModel.viewState.collectAsStateLifecycleAware()

    val rbViewModel: RecurringBuysViewModel = getViewModel(scope = payloadScope)
    val rbViewState: RecurringBuysViewState by rbViewModel.viewState.collectAsStateLifecycleAware()

    val pricesViewModel: PricesViewModel = getViewModel(scope = payloadScope)
    val pricesViewState: PricesViewState by pricesViewModel.viewState.collectAsStateLifecycleAware()

    val quickActionsViewModel: QuickActionsViewModel = getViewModel(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner,
        scope = payloadScope
    )
    val quickActionsState: QuickActionsViewState by quickActionsViewModel.viewState.collectAsStateLifecycleAware()

    val announcementsViewModel: AnnouncementsViewModel = getViewModel(scope = payloadScope)
    val announcementsState: AnnouncementsViewState by announcementsViewModel.viewState.collectAsStateLifecycleAware()

    val custodialActivityViewModel: CustodialActivityViewModel = getViewModel(scope = payloadScope)
    val custodialActivityState: ActivityViewState by custodialActivityViewModel.viewState.collectAsStateLifecycleAware()

    val pkwActivityViewModel: PrivateKeyActivityViewModel = getViewModel(scope = payloadScope)
    val pkwActivityState: ActivityViewState by pkwActivityViewModel.viewState.collectAsStateLifecycleAware()

    val referralViewModel: ReferralViewModel = getViewModel(scope = payloadScope)
    val referralState: ReferralViewState by referralViewModel.viewState.collectAsStateLifecycleAware()

    val homeDappsViewModel: HomeDappsViewModel = getViewModel(scope = payloadScope)
    val homeDappsState: HomeDappsViewState by homeDappsViewModel.viewState.collectAsStateLifecycleAware()

    val newsViewModel: NewsViewModel = getViewModel(scope = payloadScope)
    val newsViewState: NewsViewState by newsViewModel.viewState.collectAsStateLifecycleAware()

    val walletMode by
    get<WalletModeService>(scope = payloadScope).walletMode.collectAsStateLifecycleAware(initial = null)

    DisposableEffect(walletMode) {
        walletMode?.let {
            analytics.logEvent(DashboardAnalyticsEvents.ModeViewed(walletMode = it))
        }
        onDispose { }
    }

    val maxQuickActions = maxQuickActionsOnScreen

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                announcementsViewModel.onIntent(AnnouncementsIntent.LoadAnnouncements)
                homeAssetsViewModel.onIntent(AssetsIntent.LoadFilters)
                homeAssetsViewModel.onIntent(AssetsIntent.LoadAccounts(SectionSize.Limited(MAX_ASSET_COUNT)))
                homeAssetsViewModel.onIntent(AssetsIntent.LoadFundLocks)
                rbViewModel.onIntent(RecurringBuysIntent.LoadRecurringBuys(SectionSize.Limited(MAX_RB_COUNT)))
                quickActionsViewModel.onIntent(QuickActionsIntent.LoadActions(maxQuickActions))
                pricesViewModel.onIntent(PricesIntents.LoadData(PricesLoadStrategy.TradableOnly))
                referralViewModel.onIntent(ReferralIntent.LoadData())
                custodialActivityViewModel.onIntent(
                    ActivityIntent.LoadActivity(SectionSize.Limited(MAX_ACTIVITY_COUNT))
                )
                pkwActivityViewModel.onIntent(ActivityIntent.LoadActivity(SectionSize.Limited(MAX_ACTIVITY_COUNT)))
                homeDappsViewModel.onIntent(HomeDappsIntent.LoadData)
                //                newsViewModel.onIntent(NewsIntent.LoadData)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(key1 = isSwipingToRefresh) {
        if (isSwipingToRefresh) {
            announcementsViewModel.onIntent(AnnouncementsIntent.Refresh)
            homeAssetsViewModel.onIntent(AssetsIntent.Refresh)
            pricesViewModel.onIntent(PricesIntents.Refresh)
            quickActionsViewModel.onIntent(QuickActionsIntent.Refresh)
            pkwActivityViewModel.onIntent(ActivityIntent.Refresh())
            custodialActivityViewModel.onIntent(ActivityIntent.Refresh())
            //            newsViewModel.onIntent(NewsIntent.Refresh)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(
                AppTheme.colors.backgroundMuted
            )
    ) {
        stickyHeader {
            MenuOptionsScreen(
                modifier = Modifier.onGloballyPositioned {
                    menuOptionsHeight = it.size.height
                },
                walletBalance = (assetsViewState.balance.balance as? DataResource.Data)?.data?.toStringWithSymbol()
                    ?: "",
                openSettings = openSettings,
                launchQrScanner = launchQrScanner,
                showBackground = balanceOffsetToMenuOption <= 0F && menuOptionsHeight > 0F,
                showBalance = balanceScrollRange <= 0.5 && menuOptionsHeight > 0F
            )
        }

        item {
            BalanceScreen(
                modifier = Modifier.onGloballyPositioned {
                    (it.positionInParent().y - menuOptionsHeight + balanceToMenuPaddingPx)
                        .coerceAtLeast(0F).let {
                            if (balanceOffsetToMenuOption != it) balanceOffsetToMenuOption = it
                        }

                    ((it.positionInParent().y / menuOptionsHeight.toFloat()) * 2).coerceIn(0F, 1F).let {
                        if (balanceScrollRange != it) balanceScrollRange = it
                    }
                },
                balanceAlphaProvider = { balanceScrollRange },
                hideBalance = balanceScrollRange <= 0.5 && menuOptionsHeight > 0F,
                walletBalance = assetsViewState.balance

            )
        }

        quickActionsState.actions.let {
            val wMode = walletMode ?: return@let
            paddedItem(
                paddingValues = {
                    PaddingValues(AppTheme.dimensions.smallSpacing)
                }
            ) {
                QuickActions(
                    quickActionItems = it,
                    assetActionsNavigation = assetActionsNavigation,
                    quickActionsViewModel = quickActionsViewModel,
                    openDexSwapOptions = openSwapDexOption,
                    dashboardState = dashboardState(
                        assetsViewState,
                        when (wMode) {
                            WalletMode.CUSTODIAL -> custodialActivityState
                            WalletMode.NON_CUSTODIAL -> pkwActivityState
                        }
                    ),
                    openMoreQuickActions = openMoreQuickActions,
                    openSwap = openSwap
                )
            }
        }

        item {
            (announcementsState.remoteAnnouncements as? DataResource.Data)?.data?.let { announcements ->
                StackedAnnouncements(
                    announcements = announcements,
                    hideConfirmation = announcementsState.hideAnnouncementsConfirmation,
                    animateHideConfirmation = announcementsState.animateHideAnnouncementsConfirmation,
                    announcementOnSwiped = { announcement ->
                        announcementsViewModel.onIntent(AnnouncementsIntent.DeleteAnnouncement(announcement))
                    },
                    announcementOnClick = { announcement ->
                        processAnnouncementUrl(announcement.actionUrl)
                        announcementsViewModel.onIntent(AnnouncementsIntent.AnnouncementClicked(announcement))
                    }
                )
            }
        }

        announcementsState.localAnnouncements.takeIf { it.isNotEmpty() }?.let { localAnnouncements ->
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

        walletMode?.let {
            emptyCard(
                walletMode = it,
                assetsViewState = assetsViewState,
                actiityViewState = when (it) {
                    WalletMode.CUSTODIAL -> custodialActivityState
                    WalletMode.NON_CUSTODIAL -> pkwActivityState
                },
                assetActionsNavigation = assetActionsNavigation
            )
        }

        val assets = (assetsViewState.assets as? DataResource.Data)?.data
        val locks = (assetsViewState.fundsLocks as? DataResource.Data)?.data

        assets?.takeIf { it.isNotEmpty() }?.let { data ->
            homeAssets(
                locks = locks,
                data = assets,
                openCryptoAssets = {
                    openCryptoAssets()
                    analytics.logEvent(DashboardAnalyticsEvents.AssetsSeeAllClicked(assetsCount = data.size))
                },
                assetOnClick = { asset ->
                    assetActionsNavigation.coinview(asset)
                    analytics.logEvent(DashboardAnalyticsEvents.CryptoAssetClicked(ticker = asset.displayTicker))
                },
                fundsLocksOnClick = { fundsLocks ->
                    assetActionsNavigation.fundsLocksDetail(fundsLocks)
                },
                openFiatActionDetail = { ticker ->
                    openFiatActionDetail(ticker)
                    analytics.logEvent(DashboardAnalyticsEvents.FiatAssetClicked(ticker = ticker))
                }
            )
        }

        if (walletMode == WalletMode.NON_CUSTODIAL) {
            homeDapps(
                homeDappsState = homeDappsState,
                openQrCodeScanner = launchQrScanner,
                onDappSessionClicked = onWalletConnectSessionClicked,
                onWalletConnectSeeAllSessionsClicked = onWalletConnectSeeAllSessionsClicked,
            )
        }

        // recurring buys
        if (walletMode == WalletMode.CUSTODIAL) {
            rbViewState.recurringBuys
                .map { state ->
                    (state as? RecurringBuyEligibleState.Eligible)?.recurringBuys
                }
                .dataOrElse(null)
                ?.let { recurringBuys ->
                    homeRecurringBuys(
                        analytics = analytics,
                        recurringBuys = recurringBuys,
                        manageOnclick = openRecurringBuys,
                        upsellOnClick = recurringBuyNavigation::openOnboarding,
                        recurringBuyOnClick = openRecurringBuyDetail
                    )
                }
        }

        // top movers
        homeTopMovers(
            data = pricesViewState.topMovers.toImmutableList(),
            assetOnClick = { asset ->
                assetActionsNavigation.coinview(asset)

                pricesViewState.topMovers.percentAndPositionOf(asset)?.let { (percentageMove, position) ->
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

        walletMode?.let {
            val activityState = when (it) {
                WalletMode.CUSTODIAL -> custodialActivityState
                WalletMode.NON_CUSTODIAL -> pkwActivityState
            }
            homeActivityScreen(
                activityState,
                openActivity,
                openActivityDetail,
                it
            )
        }

        (referralState.referralInfo as? DataResource.Data)?.data?.let {
            (it as? ReferralInfo.Data)?.let {
                homeReferral(
                    referralData = it,
                    openReferral = openReferral
                )
            }
        }

        homeNews(
            data = newsViewState.newsArticles?.toImmutableList(),
            seeAllOnClick = {}
        )

        homeHelp(
            openSupportCenter = { supportNavigation.launchSupportCenter() }
        )

        item {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.borderRadiiLarge))
        }
    }
}

private const val MAX_ASSET_COUNT = 7
private const val MAX_ACTIVITY_COUNT = 5
private const val MAX_RB_COUNT = 5
