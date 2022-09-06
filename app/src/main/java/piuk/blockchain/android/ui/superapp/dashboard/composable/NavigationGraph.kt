package piuk.blockchain.android.ui.superapp.dashboard.composable

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.SnackbarDefaults.backgroundColor
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import piuk.blockchain.android.R

@Composable
fun NavigationGraph(modifier: Modifier = Modifier, navController: NavHostController, indexedChanged: (Pair<Int, Int>) -> Unit) {
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
    modifier : Modifier = Modifier,
    navController: NavController,
    onSelected : (BottomNavItem)-> Unit
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Card,
    )
    BottomNavigation(
        modifier = modifier,
        backgroundColor = colorResource(id = R.color.teal_200),
        contentColor = Color.Black,

    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { item ->
            BottomNavigationItem(
                icon = { Icon(painterResource(id = item.icon), contentDescription = item.title) },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 9.sp
                    )
                },
                selectedContentColor = Color.Black,
                unselectedContentColor = Color.Black.copy(0.4f),
                alwaysShowLabel = true,
                selected = currentRoute == item.screen_route,
                onClick = {
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
                }
            )
        }
    }
}