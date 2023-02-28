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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.chrome.MenuOptionsScreen
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.data.toImmutableList
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.accouncement.AnnouncementsIntent
import com.blockchain.home.presentation.accouncement.AnnouncementsViewModel
import com.blockchain.home.presentation.accouncement.AnnouncementsViewState
import com.blockchain.home.presentation.accouncement.CustomAnnouncementType
import com.blockchain.home.presentation.accouncement.composable.LocalAnnouncements
import com.blockchain.home.presentation.accouncement.composable.StackedAnnouncements
import com.blockchain.home.presentation.activity.list.ActivityIntent
import com.blockchain.home.presentation.activity.list.ActivityViewState
import com.blockchain.home.presentation.activity.list.custodial.CustodialActivityViewModel
import com.blockchain.home.presentation.activity.list.privatekey.PrivateKeyActivityViewModel
import com.blockchain.home.presentation.allassets.AssetsIntent
import com.blockchain.home.presentation.allassets.AssetsViewModel
import com.blockchain.home.presentation.allassets.AssetsViewState
import com.blockchain.home.presentation.dashboard.DashboardAnalyticsEvents
import com.blockchain.home.presentation.earn.EarnIntent
import com.blockchain.home.presentation.earn.EarnNavEvent
import com.blockchain.home.presentation.earn.EarnType
import com.blockchain.home.presentation.earn.EarnViewModel
import com.blockchain.home.presentation.earn.EarnViewState
import com.blockchain.home.presentation.earn.homeEarnAssets
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.home.presentation.navigation.SupportNavigation
import com.blockchain.home.presentation.quickactions.QuickActions
import com.blockchain.home.presentation.quickactions.QuickActionsIntent
import com.blockchain.home.presentation.quickactions.QuickActionsViewModel
import com.blockchain.home.presentation.quickactions.QuickActionsViewState
import com.blockchain.home.presentation.quickactions.maxQuickActionsOnScreen
import com.blockchain.home.presentation.referral.ReferralIntent
import com.blockchain.home.presentation.referral.ReferralViewModel
import com.blockchain.home.presentation.referral.ReferralViewState
import com.blockchain.koin.payloadScope
import com.blockchain.prices.prices.PricesIntents
import com.blockchain.prices.prices.PricesLoadStrategy
import com.blockchain.prices.prices.PricesViewModel
import com.blockchain.prices.prices.PricesViewState
import com.blockchain.prices.prices.percentAndPositionOf
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    analytics: Analytics = get(),
    listState: LazyListState,
    isSwipingToRefresh: Boolean,
    assetActionsNavigation: AssetActionsNavigation,
    supportNavigation: SupportNavigation,
    openSettings: () -> Unit,
    launchQrScanner: () -> Unit,
    openCryptoAssets: () -> Unit,
    openActivity: () -> Unit,
    openActivityDetail: (String, WalletMode) -> Unit,
    openReferral: () -> Unit,
    openSwapDexOption: () -> Unit,
    openFiatActionDetail: (String) -> Unit,
    openMoreQuickActions: () -> Unit,
    startPhraseRecovery: () -> Unit,
    openEarnDashboard: () -> Unit,
) {
    var menuOptionsHeight: Int by remember { mutableStateOf(0) }
    var balanceOffsetToMenuOption: Float by remember { mutableStateOf(0F) }
    val balanceToMenuPaddingPx: Int = LocalDensity.current.run { 24.dp.toPx() }.toInt()
    var balanceScrollRange: Float by remember { mutableStateOf(0F) }

    val lifecycleOwner = LocalLifecycleOwner.current

    val homeAssetsViewModel: AssetsViewModel = getViewModel(scope = payloadScope)
    val assetsViewState: AssetsViewState by homeAssetsViewModel.viewState.collectAsStateLifecycleAware()

    val pricesViewModel: PricesViewModel = getViewModel(scope = payloadScope)
    val pricesViewState: PricesViewState by pricesViewModel.viewState.collectAsStateLifecycleAware()

    val earnViewModel: EarnViewModel = getViewModel(scope = payloadScope)
    val earnViewState: EarnViewState by earnViewModel.viewState.collectAsStateLifecycleAware()

    val quickActionsViewModel: QuickActionsViewModel = getViewModel(scope = payloadScope)
    val quickActionsState: QuickActionsViewState by quickActionsViewModel.viewState.collectAsStateLifecycleAware()

    val announcementsViewModel: AnnouncementsViewModel = getViewModel(scope = payloadScope)
    val announcementsState: AnnouncementsViewState by announcementsViewModel.viewState.collectAsStateLifecycleAware()

    val custodialActivityViewModel: CustodialActivityViewModel = getViewModel(scope = payloadScope)
    val custodialActivityState: ActivityViewState by custodialActivityViewModel.viewState.collectAsStateLifecycleAware()

    val pkwActivityViewModel: PrivateKeyActivityViewModel = getViewModel(scope = payloadScope)
    val pkwActivityState: ActivityViewState by pkwActivityViewModel.viewState.collectAsStateLifecycleAware()

    val referralViewModel: ReferralViewModel = getViewModel(scope = payloadScope)
    val referralState: ReferralViewState by referralViewModel.viewState.collectAsStateLifecycleAware()

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
                pricesViewModel.onIntent(PricesIntents.LoadData(PricesLoadStrategy.TradableOnly))
                earnViewModel.onIntent(EarnIntent.LoadEarnAccounts())
                quickActionsViewModel.onIntent(QuickActionsIntent.LoadActions(maxQuickActions))
                referralViewModel.onIntent(ReferralIntent.LoadData())
                custodialActivityViewModel.onIntent(
                    ActivityIntent.LoadActivity(SectionSize.Limited(MAX_ACTIVITY_COUNT))
                )
                pkwActivityViewModel.onIntent(ActivityIntent.LoadActivity(SectionSize.Limited(MAX_ACTIVITY_COUNT)))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val navEventsFlowLifecycleAware = remember(earnViewModel.navigationEventFlow, lifecycleOwner) {
        earnViewModel.navigationEventFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }

    // TODO (labreu) this should be done inside homeEarnAssets but it's not a composable
    LaunchedEffect(key1 = earnViewModel) {
        navEventsFlowLifecycleAware.collectLatest {
            when (it) {
                is EarnNavEvent.Interest -> {
                    assetActionsNavigation.interestSummary(it.account)
                    analytics.logEvent(
                        DashboardAnalyticsEvents.EarnAssetClicked(
                            currency = it.account.currency.networkTicker,
                            product = EarnType.INTEREST
                        )
                    )
                }
                is EarnNavEvent.Staking -> {
                    assetActionsNavigation.stakingSummary(it.account.currency.networkTicker)
                    analytics.logEvent(
                        DashboardAnalyticsEvents.EarnAssetClicked(
                            currency = it.account.currency.networkTicker,
                            product = EarnType.STAKING
                        )
                    )
                }
                is EarnNavEvent.ActiveRewards -> {
                    assetActionsNavigation.activeRewardsSummary(it.account.currency.networkTicker)
                    analytics.logEvent(
                        DashboardAnalyticsEvents.EarnAssetClicked(
                            currency = it.account.currency.networkTicker,
                            product = EarnType.ACTIVE
                        )
                    )
                }
            }
        }
    }

    LaunchedEffect(key1 = isSwipingToRefresh) {
        if (isSwipingToRefresh) {
            announcementsViewModel.onIntent(AnnouncementsIntent.Refresh)
            homeAssetsViewModel.onIntent(AssetsIntent.Refresh)
            pricesViewModel.onIntent(PricesIntents.Refresh)
            earnViewModel.onIntent(EarnIntent.Refresh)
            quickActionsViewModel.onIntent(QuickActionsIntent.Refresh)
            pkwActivityViewModel.onIntent(ActivityIntent.Refresh())
            custodialActivityViewModel.onIntent(ActivityIntent.Refresh())
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(
                AppTheme.colors.backgroundMuted
            ),
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
                paddingValues = PaddingValues(horizontal = 16.dp)
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
                )
            }
        }

        item {
            (announcementsState.stackedAnnouncements as? DataResource.Data)?.data?.let { announcements ->
                StackedAnnouncements(
                    announcements = announcements,
                    onSwiped = { targetId ->
                        //                    announcements.removeIf { it.id == targetId }
                    }
                )
            }
        }

        paddedItem(
            paddingValues = PaddingValues(horizontal = 16.dp)
        ) {
            LocalAnnouncements(
                announcements = announcementsState.customAnnouncements,
                onClick = { announcement ->
                    when (announcement.type) {
                        CustomAnnouncementType.PHRASE_RECOVERY -> startPhraseRecovery()
                    }
                }
            )
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
            },
        )

        earnViewState.let { earnState ->
            homeEarnAssets(earnState = earnState, earnViewModel = earnViewModel, openEarnDashboard = openEarnDashboard)
        }

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
                paddedItem(
                    paddingValues = PaddingValues(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))
                    ReferralComponent(
                        openReferral = openReferral,
                        referralData = it
                    )
                }
            }
        }

        paddedItem(
            paddingValues = PaddingValues(horizontal = 16.dp)
        ) {
            HelpAndSupport(
                openSupportCenter = { supportNavigation.launchSupportCenter() }
            )
        }

        item {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.borderRadiiLarge))
        }
    }
}

private const val MAX_ASSET_COUNT = 7
private const val MAX_ACTIVITY_COUNT = 5
