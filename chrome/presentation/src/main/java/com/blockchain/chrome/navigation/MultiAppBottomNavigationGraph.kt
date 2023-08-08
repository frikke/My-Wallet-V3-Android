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
import com.blockchain.componentlib.chrome.ChromeGridScreen
import com.blockchain.componentlib.chrome.ChromeListScreen
import com.blockchain.componentlib.chrome.ListStateInfo
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.earn.dashboard.EarnDashboardScreen
import com.blockchain.earn.navigation.EarnNavigation
import com.blockchain.home.presentation.dashboard.composable.HomeScreen
import com.blockchain.home.presentation.navigation.QrScanNavigation
import com.blockchain.koin.payloadScope
import com.blockchain.nfts.collection.screen.NftCollection
import com.blockchain.prices.prices.composable.Prices
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import com.dex.presentation.enteramount.DexEnterAmountScreen
import org.koin.androidx.compose.get

@Composable
fun MultiAppBottomNavigationHost(
    modifier: Modifier = Modifier,
    navControllerProvider: () -> NavHostController,
    navigateToMode: (WalletMode) -> Unit,
    enableRefresh: Boolean,
    qrScanNavigation: QrScanNavigation,
    updateScrollInfo: (Pair<ChromeBottomNavigationItem, ListStateInfo>) -> Unit,
    selectedNavigationItem: ChromeBottomNavigationItem,
    refreshStarted: () -> Unit,
    refreshComplete: () -> Unit,
    startPhraseRecovery: () -> Unit,
    openExternalUrl: (url: String) -> Unit,
    navController: NavController,
    openNftHelp: () -> Unit,
    openNftDetail: (nftId: String, address: String, pageKey: String?) -> Unit,
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

    val settingsNavigation = LocalSettingsNavigationProvider.current
    val openSettings = remember { { settingsNavigation.settings() } }

    val launchQrScanner = remember {
        {
            qrScanNavigation.launchQrScan()
        }
    }

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
                        openSettings = openSettings,
                        launchQrScanner = launchQrScanner,
                        startPhraseRecovery = startPhraseRecovery,
                        processAnnouncementUrl = processAnnouncementUrl,
                        navigateToMode = navigateToMode,
                    )
                }
            )
        }

        composable(ChromeBottomNavigationItem.Dex.route) {
            val assetActionsNavigation = LocalAssetActionsNavigationProvider.current

            ChromeListScreen(
                modifier = modifier,
                isPullToRefreshEnabled = enableRefresh,
                content = { listState, _ ->
                    DexEnterAmountScreen(
                        listState = listState,
                        savedStateHandle = navController.currentBackStackEntry
                            ?.savedStateHandle,
                        openSettings = openSettings,
                        launchQrScanner = launchQrScanner,
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
                        openSettings = openSettings,
                        launchQrScanner = launchQrScanner
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
                    EarnDashboardScreen(
                        earnNavigation = earnNavigation,
                        openSettings = openSettings,
                        launchQrScanner = launchQrScanner
                    )
                }
            )
        }
    }
}
