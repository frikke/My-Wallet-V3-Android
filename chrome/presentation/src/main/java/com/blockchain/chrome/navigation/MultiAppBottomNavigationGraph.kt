package com.blockchain.chrome.navigation

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.blockchain.chrome.ChromeBottomNavigationItem
import com.blockchain.componentlib.chrome.ChromeScreen
import com.blockchain.componentlib.chrome.ListStateInfo
import com.blockchain.home.presentation.dashboard.composable.HomeScreen
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.home.presentation.navigation.QrScanNavigation
import com.blockchain.home.presentation.navigation.SettingsNavigation
import com.blockchain.home.presentation.navigation.SupportNavigation
import com.blockchain.prices.navigation.PricesNavigation
import com.blockchain.prices.prices.composable.Prices
import com.blockchain.walletmode.WalletMode

@Composable
fun MultiAppBottomNavigationHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
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
    openFiatActionDetail: (String) -> Unit
) {

    val openSettings = remember { { settingsNavigation.settings() } }
    val launchQrScanner = remember { { qrScanNavigation.launchQrScan() } }

    NavHost(navController, startDestination = ChromeBottomNavigationItem.Home.route) {
        composable(ChromeBottomNavigationItem.Home.route) {
            val listState = rememberLazyListState()
            ChromeScreen(
                modifier = modifier,
                updateScrollInfo = { updateScrollInfo(Pair(ChromeBottomNavigationItem.Home, it)) },
                isPullToRefreshEnabled = enableRefresh,
                content = { shouldTriggerRefresh ->
                    HomeScreen(
                        listState = listState,
                        shouldTriggerRefresh = shouldTriggerRefresh &&
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
                listState = listState,
                refreshStarted = refreshStarted,
                refreshComplete = refreshComplete
            )
        }
        composable(ChromeBottomNavigationItem.Prices.route) {
            val listState = rememberLazyListState()
            ChromeScreen(
                modifier = modifier,
                updateScrollInfo = { updateScrollInfo(Pair(ChromeBottomNavigationItem.Prices, it)) },
                isPullToRefreshEnabled = enableRefresh,
                content = {
                    Prices(
                        listState = listState,
                        pricesNavigation = pricesNavigation,
                        openSettings = openSettings,
                        launchQrScanner = launchQrScanner,
                    )
                },
                listState = listState,
                refreshStarted = refreshStarted,
                refreshComplete = refreshComplete
            )
        }
    }
}
