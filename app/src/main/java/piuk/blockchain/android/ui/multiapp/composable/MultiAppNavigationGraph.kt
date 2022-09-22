package piuk.blockchain.android.ui.multiapp.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import piuk.blockchain.android.ui.multiapp.ChromeBottomNavigationItem

@Composable
fun MultiAppNavigationGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    enableRefresh: Boolean,
    updateScrollInfo: (Pair<ChromeBottomNavigationItem, ListStateInfo>) -> Unit,
    refreshStarted: () -> Unit,
    refreshComplete: () -> Unit
) {
    NavHost(navController, startDestination = ChromeBottomNavigationItem.Home.route) {
        composable(ChromeBottomNavigationItem.Home.route) {
            DemoScreen(
                modifier = modifier,
                tag = "Home",
                updateScrollInfo = { updateScrollInfo(Pair(ChromeBottomNavigationItem.Home, it)) },
                isPullToRefreshEnabled = enableRefresh,
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
