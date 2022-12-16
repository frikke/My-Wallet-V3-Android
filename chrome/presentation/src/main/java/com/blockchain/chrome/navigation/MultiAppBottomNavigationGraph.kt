package com.blockchain.chrome.navigation

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
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
import com.blockchain.prices.navigation.PricesNavigation
import com.blockchain.prices.prices.composable.Prices

@Composable
fun MultiAppBottomNavigationHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    enableRefresh: Boolean,
    assetActionsNavigation: AssetActionsNavigation,
    settingsNavigation: SettingsNavigation,
    pricesNavigation: PricesNavigation,
    qrScanNavigation: QrScanNavigation,
    updateScrollInfo: (Pair<ChromeBottomNavigationItem, ListStateInfo>) -> Unit,
    refreshStarted: () -> Unit,
    refreshComplete: () -> Unit,
    openCryptoAssets: () -> Unit,
    openActivity: () -> Unit,
    openReferral: () -> Unit,
    openMoreQuickActions: () -> Unit,
    openFiatActionDetail: (String) -> Unit
) {
    NavHost(navController, startDestination = ChromeBottomNavigationItem.Home.route) {
        composable(ChromeBottomNavigationItem.Home.route) {
            val listState = rememberLazyListState()
            ChromeScreen(
                modifier = modifier,
                updateScrollInfo = { updateScrollInfo(Pair(ChromeBottomNavigationItem.Home, it)) },
                isPullToRefreshEnabled = enableRefresh,
                content = {
                    HomeScreen(
                        listState = listState,
                        openCryptoAssets = openCryptoAssets,
                        assetActionsNavigation = assetActionsNavigation,
                        qrScanNavigation = qrScanNavigation,
                        settingsNavigation = settingsNavigation,
                        openActivity = openActivity,
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
                updateScrollInfo = { updateScrollInfo(Pair(ChromeBottomNavigationItem.Home, it)) },
                isPullToRefreshEnabled = enableRefresh,
                content = {
                    Prices(
                        listState = listState,
                        pricesNavigation = pricesNavigation,
                    )
                },
                listState = listState,
                refreshStarted = refreshStarted,
                refreshComplete = refreshComplete
            )
        }
    }
}
