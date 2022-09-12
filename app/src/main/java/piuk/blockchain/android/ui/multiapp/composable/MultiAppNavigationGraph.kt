package piuk.blockchain.android.ui.multiapp.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.blockchain.componentlib.basic.ImageResource
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.multiapp.bottomnav.BottomNavItem

@Composable
fun MultiAppNavigationGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    enableRefresh: Boolean,
    updateScrollInfo: (Pair<BottomNavItem, ListStateInfo>) -> Unit,
    refreshStarted: () -> Unit,
    refreshComplete: () -> Unit
) {
    NavHost(navController, startDestination = BottomNavItem.Home.screen_route) {
        composable(BottomNavItem.Home.screen_route) {
            DemoScreen(
                modifier = modifier,
                tag = "Home",
                updateScrollInfo = { updateScrollInfo(Pair(BottomNavItem.Home, it)) },
                isPullToRefreshEnabled = enableRefresh,
                refreshStarted = refreshStarted,
                refreshComplete = refreshComplete
            )
        }
        composable(BottomNavItem.Trade.screen_route) {
            DemoScreen(
                modifier = modifier,
                tag = "Trade",
                updateScrollInfo = { updateScrollInfo(Pair(BottomNavItem.Trade, it)) },
                isPullToRefreshEnabled = enableRefresh,
                refreshStarted = refreshStarted,
                refreshComplete = refreshComplete
            )
        }
        composable(BottomNavItem.Card.screen_route) {
            DemoScreen(
                modifier = modifier,
                tag = "Card",
                updateScrollInfo = { updateScrollInfo(Pair(BottomNavItem.Card, it)) },
                isPullToRefreshEnabled = enableRefresh,
                refreshStarted = refreshStarted,
                refreshComplete = refreshComplete
            )
        }
    }
}

// todo(othman) create real BottomNavItem items and support multi app modes
@Composable
fun MultiAppBottomNavigation(
    modifier: Modifier = Modifier,
    navigationItem: List<BottomNavItem>,
    navController: NavController,
    onSelected: (BottomNavItem) -> Unit
) {
    Card(
        modifier = modifier,
        elevation = 15.dp,
        contentColor = Color.White,
        shape = RoundedCornerShape(100.dp)
    ) {
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            Spacer(Modifier.size(dimensionResource(R.dimen.large_margin)))

            navigationItem.forEach { item ->

                com.blockchain.componentlib.basic.Image(
                    modifier = Modifier.clickable {
                        onSelected(item)
                        navController.navigate(item.screen_route) {

                            navController.graph.startDestinationRoute?.let { screen_route ->
                                popUpTo(screen_route) {
                                    saveState = true
                                }
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    imageResource = ImageResource.Local(item.icon)
                )

                Spacer(Modifier.size(dimensionResource(R.dimen.large_margin)))
            }
        }
    }
}