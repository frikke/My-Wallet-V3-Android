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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.R
import com.blockchain.componentlib.chrome.MenuOptionsScreen
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.activity.list.ActivityViewState
import com.blockchain.home.presentation.activity.list.TransactionGroup
import com.blockchain.home.presentation.allassets.AssetsIntent
import com.blockchain.home.presentation.allassets.AssetsViewModel
import com.blockchain.home.presentation.allassets.AssetsViewState
import com.blockchain.home.presentation.dashboard.DashboardAnalyticsEvents
import com.blockchain.home.presentation.earn.EarnIntent
import com.blockchain.home.presentation.earn.EarnNavEvent
import com.blockchain.home.presentation.earn.EarnViewModel
import com.blockchain.home.presentation.earn.EarnViewState
import com.blockchain.home.presentation.earn.HomeEarnHeader
import com.blockchain.home.presentation.earn.homeEarnAssets
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.home.presentation.navigation.SupportNavigation
import com.blockchain.home.presentation.quickactions.QuickActions
import com.blockchain.home.presentation.quickactions.QuickActionsIntent
import com.blockchain.home.presentation.quickactions.QuickActionsViewModel
import com.blockchain.home.presentation.quickactions.QuickActionsViewState
import com.blockchain.home.presentation.referral.ReferralIntent
import com.blockchain.home.presentation.referral.ReferralViewModel
import com.blockchain.home.presentation.referral.ReferralViewState
import com.blockchain.koin.payloadScope
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
    shouldTriggerRefresh: Boolean,
    assetActionsNavigation: AssetActionsNavigation,
    supportNavigation: SupportNavigation,
    openSettings: () -> Unit,
    launchQrScanner: () -> Unit,
    openCryptoAssets: () -> Unit,
    openActivity: () -> Unit,
    openActivityDetail: (String, WalletMode) -> Unit,
    openReferral: () -> Unit,
    openFiatActionDetail: (String) -> Unit,
    openMoreQuickActions: () -> Unit,
) {

    var menuOptionsHeight: Int by remember { mutableStateOf(0) }
    var balanceOffsetToMenuOption: Float by remember { mutableStateOf(0F) }
    val balanceToMenuPaddingPx: Int = LocalDensity.current.run { 24.dp.toPx() }.toInt()
    var balanceScrollRange: Float by remember { mutableStateOf(0F) }

    val lifecycleOwner = LocalLifecycleOwner.current

    val homeAssetsViewModel: AssetsViewModel = getViewModel(scope = payloadScope)
    val homeViewState: AssetsViewState by homeAssetsViewModel.viewState.collectAsStateLifecycleAware()

    val earnViewModel: EarnViewModel = getViewModel(scope = payloadScope)
    val earnViewState: EarnViewState by earnViewModel.viewState.collectAsStateLifecycleAware()

    val quickActionsViewModel: QuickActionsViewModel = getViewModel(scope = payloadScope)
    val quickActionsState: QuickActionsViewState by quickActionsViewModel.viewState.collectAsStateLifecycleAware()

    val homeActivityViewModel: HomeActivityViewModel = getViewModel(scope = payloadScope)
    val activityState: ActivityViewState? by homeActivityViewModel.state().collectAsStateLifecycleAware(null)

    val referralViewModel: ReferralViewModel = getViewModel(scope = payloadScope)
    val referralState: ReferralViewState by referralViewModel.viewState.collectAsStateLifecycleAware()

    val walletMode by
    get<WalletModeService>(scope = payloadScope).walletMode.collectAsStateLifecycleAware(initial = null)

    DisposableEffect(walletMode) {
        walletMode?.let {
            analytics.logEvent(DashboardAnalyticsEvents.ModeViewed(walletMode  = it))
        }
        onDispose { }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeAssetsViewModel.onIntent(AssetsIntent.LoadFilters)
                homeAssetsViewModel.onIntent(AssetsIntent.LoadAccounts(SectionSize.Limited(MAX_ASSET_COUNT)))
                homeAssetsViewModel.onIntent(AssetsIntent.LoadFundLocks)
                earnViewModel.onIntent(EarnIntent.LoadEarnAccounts())
                quickActionsViewModel.onIntent(QuickActionsIntent.LoadActions)
                referralViewModel.onIntent(ReferralIntent.LoadData())
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

    LaunchedEffect(key1 = earnViewModel) {
        navEventsFlowLifecycleAware.collectLatest {
            when (it) {
                is EarnNavEvent.Interest -> assetActionsNavigation.interestSummary(it.account)
                is EarnNavEvent.Staking -> assetActionsNavigation.stakingSummary(it.account.currency)
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = AppTheme.dimensions.smallSpacing),
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
                walletBalance = (homeViewState.balance.balance as? DataResource.Data)?.data?.toStringWithSymbol() ?: "",
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
                walletBalance = homeViewState.balance

            )
        }

        quickActionsState.actions.let {
            item {
                QuickActions(
                    quickActionItems = it,
                    assetActionsNavigation = assetActionsNavigation,
                    quickActionsViewModel = quickActionsViewModel,
                    openMoreQuickActions = openMoreQuickActions,
                )
            }
        }

        emptyCard(wMode = walletMode, homeViewState, activityState, assetActionsNavigation)

        val assets = (homeViewState.assets as? DataResource.Data)?.data
        val locks = (homeViewState.fundsLocks as? DataResource.Data)?.data

        assets?.takeIf { it.isNotEmpty() }?.let { data ->

            item {
                Spacer(modifier = Modifier.size(dimensionResource(R.dimen.large_spacing)))
                HomeAssetsHeader {
                    openCryptoAssets()
                    analytics.logEvent(DashboardAnalyticsEvents.AssetsSeeAllClicked(assetsCount = data.size))
                }
                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
            }

            locks?.let {
                item {
                    FundLocksData(
                        total = locks.onHoldTotalAmount,
                        onClick = { assetActionsNavigation.fundsLocksDetail(it) }
                    )
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
                }
            }

            homeAssets(
                data = assets,
                assetOnClick = { asset ->
                    assetActionsNavigation.coinview(asset)
                    analytics.logEvent(DashboardAnalyticsEvents.CryptoAssetClicked(ticker = asset.displayTicker))
                },
                openFiatActionDetail = { ticker ->
                    openFiatActionDetail(ticker)
                    analytics.logEvent(DashboardAnalyticsEvents.FiatAssetClicked(ticker = ticker))
                }
            )
        }

        earnViewState.let { earnState ->
            if (earnState == EarnViewState.None) {
                return@let
            }
            item {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))
                HomeEarnHeader(hasAssets = earnState is EarnViewState.Assets) {
                    assetActionsNavigation.earnRewards()
                }
                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
            }
            homeEarnAssets(earnState, assetActionsNavigation, earnViewModel)
        }

        (activityState?.activity as? DataResource.Data)?.data?.get(TransactionGroup.Combined)?.takeIf {
            it.isNotEmpty()
        }?.let { activities ->
            val wMode = activityState?.walletMode ?: return@let
            item {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))
                HomeActivityHeader(openActivity = openActivity)
                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
            }
            homeActivityScreen(activities, openActivityDetail, wMode)
        }

        (referralState.referralInfo as? DataResource.Data)?.data?.let {
            (it as? ReferralInfo.Data)?.let {
                item {
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))
                    ReferralComponent(
                        openReferral = openReferral,
                        referralData = it
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))
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
