package com.blockchain.commonarch.presentation.mvi_v2.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.createGraph
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter

interface ComposeNavigationRouter : NavigationRouter<ComposeNavigationEvent> {
    var navController: NavHostController
}

/* TODO
    How can we provide this as an interface and keep the "name" parameter while keeping the code clean?
   (by clean I mean -> not forcing implementation classes to override the "name" in the class body.
    Would be cleaner if it could be overridden in the constructor itself)
*/
open class ComposeNavigationEvent(val name: String) : NavigationEvent

@Composable
fun MviNavHost(
    navigationRouter: ComposeNavigationRouter,
    startDestination: String,
    modifier: Modifier = Modifier,
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
) {
    navigationRouter.navController = rememberNavController()
    NavHost(
        navigationRouter.navController,
        remember(route, startDestination, builder) {
            navigationRouter.navController.createGraph(startDestination, route, builder)
        },
        modifier
    )
}