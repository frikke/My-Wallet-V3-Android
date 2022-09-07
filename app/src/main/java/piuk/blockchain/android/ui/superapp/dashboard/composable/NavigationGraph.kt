package piuk.blockchain.android.ui.superapp.dashboard.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.utils.clickableNoEffect
import piuk.blockchain.android.R

@Composable
fun NavigationGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    indexedChanged: (Pair<Int, Int>) -> Unit
) {
    NavHost(navController, startDestination = BottomNavItem.Home.screen_route) {
        composable(BottomNavItem.Home.screen_route) {
            HomeScreen(modifier, indexedChanged)
        }
        composable(BottomNavItem.Card.screen_route) {
            CardScreen(modifier, indexedChanged)
        }
    }
}

@Composable
fun BottomNavigationC(
    modifier: Modifier = Modifier,
    navController: NavController,
    onSelected: (BottomNavItem) -> Unit
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Trade,
        BottomNavItem.Card,
    )

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

            items.forEach { item ->

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

//                BottomNavigationItem(
//                    icon = { Icon(painterResource(id = item.icon), contentDescription = item.title) },
//                    label = {
//                        Text(
//                            text = item.title,
//                            fontSize = 9.sp
//                        )
//                    },
//                    selectedContentColor = Color.Black,
//                    unselectedContentColor = Color.Black.copy(0.4f),
//                    alwaysShowLabel = true,
//                    selected = currentRoute == item.screen_route,
//                    onClick = {
//                        onSelected(item)
//                        navController.navigate(item.screen_route) {
//
//                            navController.graph.startDestinationRoute?.let { screen_route ->
//                                popUpTo(screen_route) {
//                                    saveState = true
//                                }
//                            }
//                            launchSingleTop = true
//                            restoreState = true
//                        }
//                    }
//                )
            }
        }
    }
}