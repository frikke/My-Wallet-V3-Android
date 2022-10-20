package com.blockchain.chrome.navigation

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.blockchain.chrome.ChromeBottomNavigationItem
import com.blockchain.chrome.composable.DemoScreen
import com.blockchain.componentlib.chrome.ChromeScreen
import com.blockchain.componentlib.chrome.ListStateInfo
import com.blockchain.home.presentation.dashboard.composable.HomeScreen

@Composable
fun MultiAppBottomNavigationHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    enableRefresh: Boolean,
    updateScrollInfo: (Pair<ChromeBottomNavigationItem, ListStateInfo>) -> Unit,
    refreshStarted: () -> Unit,
    refreshComplete: () -> Unit,
    openCryptoAssets: () -> Unit,
    openActivity: () -> Unit
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
                            openActivity = openActivity
                    )
                },
                listState = listState,
                refreshStarted = refreshStarted,
                refreshComplete = refreshComplete
            )
        }
        composable(ChromeBottomNavigationItem.Trade.route) {
            DemoScreen(
                modifier = modifier,
                tag = "Trade",
                updateScrollInfo = { updateScrollInfo(Pair(ChromeBottomNavigationItem.Trade, it)) },
                isPullToRefreshEnabled = enableRefresh,
                refreshStarted = refreshStarted,
                refreshComplete = refreshComplete
            )
        }
        composable(ChromeBottomNavigationItem.Card.route) {
            DemoScreen(
                modifier = modifier,
                tag = "Card",
                updateScrollInfo = { updateScrollInfo(Pair(ChromeBottomNavigationItem.Card, it)) },
                isPullToRefreshEnabled = enableRefresh,
                refreshStarted = refreshStarted,
                refreshComplete = refreshComplete
            )
        }
        composable(ChromeBottomNavigationItem.Nft.route) {
            DemoScreen(
                modifier = modifier,
                tag = "NFT",
                updateScrollInfo = { updateScrollInfo(Pair(ChromeBottomNavigationItem.Nft, it)) },
                isPullToRefreshEnabled = enableRefresh,
                refreshStarted = refreshStarted,
                refreshComplete = refreshComplete
            )
        }
    }
}
