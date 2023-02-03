package com.blockchain.chrome.navigation

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.blockchain.chrome.ChromeBottomNavigationItem
import com.blockchain.chrome.composable.bottomNavigationItems
import com.blockchain.componentlib.chrome.ChromeScreen
import com.blockchain.componentlib.chrome.ListStateInfo
import com.blockchain.componentlib.chrome.extractStatesInfo
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
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
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
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
    openMoreQuickActions: () -> Unit,
    openFiatActionDetail: (String) -> Unit,
    openExternalUrl: (url: String) -> Unit,
    openNftHelp: () -> Unit,
    openNftDetail: (nftId: String, address: String, pageKey: String?) -> Unit,
    nftNavigation: NftNavigation,
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
            val listState = rememberLazyListState()
            var isRefreshing by remember { mutableStateOf(false) }
            val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

            updateScrollInfo(
                Pair(
                    ChromeBottomNavigationItem.Home,
                    extractStatesInfo(listState, swipeRefreshState)
                )
            )

            ChromeScreen(
                modifier = modifier,
                isPullToRefreshEnabled = enableRefresh,
                isRefreshing = isRefreshing,
                swipeRefreshState = swipeRefreshState,
                content = { shouldTriggerRefresh ->
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
                        openFiatActionDetail = openFiatActionDetail,
                        openMoreQuickActions = openMoreQuickActions
                    )
                },
                refreshStarted = {
                    isRefreshing = true
                    refreshStarted()
                },
                refreshComplete = {
                    refreshComplete()
                    isRefreshing = false
                }
            )
        }
        composable(ChromeBottomNavigationItem.Prices.route) {
            val listState = rememberLazyListState()
            var isRefreshing by remember { mutableStateOf(false) }
            val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

            updateScrollInfo(
                Pair(
                    ChromeBottomNavigationItem.Home,
                    extractStatesInfo(listState, swipeRefreshState)
                )
            )

            ChromeScreen(
                modifier = modifier,
                isPullToRefreshEnabled = enableRefresh,
                isRefreshing = isRefreshing,
                swipeRefreshState = swipeRefreshState,
                content = { shouldTriggerRefresh ->
                    Prices(
                        listState = listState,
                        shouldTriggerRefresh = shouldTriggerRefresh &&
                            selectedNavigationItem == ChromeBottomNavigationItem.Prices,
                        pricesNavigation = pricesNavigation,
                        openSettings = openSettings,
                        launchQrScanner = launchQrScanner,
                    )
                },
                refreshStarted = {
                    isRefreshing = true
                    refreshStarted()
                },
                refreshComplete = {
                    refreshComplete()
                    isRefreshing = false
                }
            )
        }
        composable(ChromeBottomNavigationItem.Nft.route) {
            val gridState = rememberLazyGridState()
            var isRefreshing by remember { mutableStateOf(false) }
            val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

            updateScrollInfo(
                Pair(
                    ChromeBottomNavigationItem.Home,
                    extractStatesInfo(gridState, swipeRefreshState)
                )
            )

            ChromeScreen(
                modifier = modifier,
                isPullToRefreshEnabled = enableRefresh,
                isRefreshing = isRefreshing,
                swipeRefreshState = swipeRefreshState,
                content = { shouldTriggerRefresh ->
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
                },
                refreshStarted = {
                    isRefreshing = true
                    refreshStarted()
                },
                refreshComplete = {
                    refreshComplete()
                    isRefreshing = false
                }
            )
        }
    }
}
