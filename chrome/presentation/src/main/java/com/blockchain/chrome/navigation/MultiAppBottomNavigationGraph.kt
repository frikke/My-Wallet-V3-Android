package com.blockchain.chrome.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.blockchain.chrome.ChromeBottomNavigationItem
import com.blockchain.chrome.composable.bottomNavigationItems
import com.blockchain.coincore.AssetAction
import com.blockchain.componentlib.chrome.ChromeGridScreen
import com.blockchain.componentlib.chrome.ChromeListScreen
import com.blockchain.componentlib.chrome.ListStateInfo
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.earn.dashboard.EarnDashboardScreen
import com.blockchain.earn.navigation.EarnNavigation
import com.blockchain.home.presentation.dashboard.composable.HomeScreen
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.home.presentation.navigation.QrScanNavigation
import com.blockchain.home.presentation.navigation.SettingsNavigation
import com.blockchain.home.presentation.navigation.SupportNavigation
import com.blockchain.koin.payloadScope
import com.blockchain.nfts.collection.screen.NftCollection
import com.blockchain.nfts.navigation.NftNavigation
import com.blockchain.prices.navigation.PricesNavigation
import com.blockchain.prices.prices.composable.Prices
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import com.dex.presentation.DexEnterAmountScreen
import org.koin.androidx.compose.get

@Composable
fun MultiAppBottomNavigationHost(
    modifier: Modifier = Modifier,
    navControllerProvider: () -> NavHostController,
    enableRefresh: Boolean,
    assetActionsNavigation: AssetActionsNavigation,
    settingsNavigation: SettingsNavigation,
    pricesNavigation: PricesNavigation,
    qrScanNavigation: QrScanNavigation,
    supportNavigation: SupportNavigation,
    updateScrollInfo: (Pair<ChromeBottomNavigationItem, ListStateInfo>) -> Unit,
    selectedNavigationItem: ChromeBottomNavigationItem,
    refreshStarted: () -> Unit,
    refreshComplete: () -> Unit,
    openCryptoAssets: () -> Unit,
    openActivity: () -> Unit,
    openActivityDetail: (String, WalletMode) -> Unit,
    openReferral: () -> Unit,
    openSwapDexOption: () -> Unit,
    openMoreQuickActions: () -> Unit,
    openFiatActionDetail: (String) -> Unit,
    startPhraseRecovery: () -> Unit,
    openExternalUrl: (url: String) -> Unit,
    navController: NavController,
    openNftHelp: () -> Unit,
    openNftDetail: (nftId: String, address: String, pageKey: String?) -> Unit,
    nftNavigation: NftNavigation,
    earnNavigation: EarnNavigation,
    processAnnouncementUrl: (String) -> Unit,
) {

    val walletMode by get<WalletModeService>(scope = payloadScope)
        .walletMode.collectAsStateLifecycleAware(initial = null)

    // example: defi (nft page) -> custodial,
    // custodial doesn't support nfts so auto navigate to home
    DisposableEffect(key1 = walletMode) {
        walletMode?.let { walletMode ->
            if (walletMode.bottomNavigationItems().contains(selectedNavigationItem).not()) {
                navControllerProvider().navigate(ChromeBottomNavigationItem.Home.route) {
                    navControllerProvider().graph.startDestinationRoute?.let { screen_route ->
                        popUpTo(screen_route) {
                            saveState = true
                        }
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
        onDispose { }
    }

    val openSettings = remember { { settingsNavigation.settings() } }
    val launchQrScanner = remember { { qrScanNavigation.launchQrScan() } }

    NavHost(navControllerProvider(), startDestination = ChromeBottomNavigationItem.Home.route) {
        composable(ChromeBottomNavigationItem.Home.route) {
            ChromeListScreen(
                modifier = modifier,
                isPullToRefreshEnabled = enableRefresh,
                refreshStarted = refreshStarted,
                refreshComplete = refreshComplete,
                updateStatesInfo = { listStateInfo ->
                    updateScrollInfo(
                        Pair(
                            ChromeBottomNavigationItem.Home,
                            listStateInfo
                        )
                    )
                },
                content = { listState, shouldTriggerRefresh ->
                    HomeScreen(
                        listState = listState,
                        isSwipingToRefresh = shouldTriggerRefresh &&
                            selectedNavigationItem == ChromeBottomNavigationItem.Home,
                        openCryptoAssets = openCryptoAssets,
                        assetActionsNavigation = assetActionsNavigation,
                        supportNavigation = supportNavigation,
                        openSettings = openSettings,
                        launchQrScanner = launchQrScanner,
                        openActivity = openActivity,
                        openActivityDetail = openActivityDetail,
                        openReferral = openReferral,
                        openSwapDexOption = openSwapDexOption,
                        openFiatActionDetail = openFiatActionDetail,
                        openMoreQuickActions = openMoreQuickActions,
                        startPhraseRecovery = startPhraseRecovery,
                        processAnnouncementUrl = processAnnouncementUrl,
                    )
                }
            )
        }

        composable(ChromeBottomNavigationItem.Dex.route) {
            ChromeListScreen(
                modifier = modifier,
                isPullToRefreshEnabled = enableRefresh,
                content = { listState, _ ->
                    DexEnterAmountScreen(
                        listState = listState,
                        navController = navController,
                        savedStateHandle = navController.currentBackStackEntry
                            ?.savedStateHandle,
                        startReceiving = { assetActionsNavigation.navigate(AssetAction.Receive) }
                    )
                },
                refreshComplete = refreshComplete,
                refreshStarted = refreshStarted,
                updateStatesInfo = { listStateInfo ->
                    updateScrollInfo(
                        Pair(
                            ChromeBottomNavigationItem.Dex,
                            listStateInfo
                        )
                    )
                }
            )
        }

        composable(ChromeBottomNavigationItem.Prices.route) {
            ChromeListScreen(
                modifier = modifier,
                isPullToRefreshEnabled = enableRefresh,
                refreshStarted = refreshStarted,
                refreshComplete = refreshComplete,
                updateStatesInfo = { listStateInfo ->
                    updateScrollInfo(
                        Pair(
                            ChromeBottomNavigationItem.Prices,
                            listStateInfo
                        )
                    )
                },
                content = { listState, shouldTriggerRefresh ->
                    Prices(
                        listState = listState,
                        shouldTriggerRefresh = shouldTriggerRefresh &&
                            selectedNavigationItem == ChromeBottomNavigationItem.Prices,
                        pricesNavigation = pricesNavigation,
                        openSettings = openSettings,
                        launchQrScanner = launchQrScanner,
                    )
                }
            )
        }

        composable(ChromeBottomNavigationItem.Nft.route) {
            ChromeGridScreen(
                modifier = modifier,
                isPullToRefreshEnabled = enableRefresh,
                refreshStarted = refreshStarted,
                refreshComplete = refreshComplete,
                updateStatesInfo = { listStateInfo ->
                    updateScrollInfo(
                        Pair(
                            ChromeBottomNavigationItem.Nft,
                            listStateInfo
                        )
                    )
                },
                content = { gridState, shouldTriggerRefresh ->
                    NftCollection(
                        gridState = gridState,
                        shouldTriggerRefresh = shouldTriggerRefresh &&
                            selectedNavigationItem == ChromeBottomNavigationItem.Nft,
                        openSettings = openSettings,
                        launchQrScanner = launchQrScanner,
                        openExternalUrl = openExternalUrl,
                        openNftHelp = openNftHelp,
                        openNftDetail = openNftDetail,
                        nftNavigation = nftNavigation
                    )
                }
            )
        }

        composable(ChromeBottomNavigationItem.Earn.route) {
            ChromeListScreen(
                modifier = modifier,
                isPullToRefreshEnabled = enableRefresh,
                refreshStarted = refreshStarted,
                refreshComplete = refreshComplete,
                updateStatesInfo = { listStateInfo ->
                    updateScrollInfo(
                        Pair(
                            ChromeBottomNavigationItem.Earn,
                            listStateInfo
                        )
                    )
                },
                content = { listState, shouldTriggerRefresh ->
                    // todo change the layout so EarnDashboardScreen is rooted with a lazylist to pass listState
                    EarnDashboardScreen(earnNavigation = earnNavigation)
                }
            )
        }
    }
}
