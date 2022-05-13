package com.blockchain.commonarch.presentation.mvi_v2.compose

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.createGraph
import androidx.navigation.get
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.componentlib.R
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

interface ComposeNavigationRouter<TNavEvent : NavigationEvent> : NavigationRouter<TNavEvent> {
    val navController: NavHostController
}

interface ComposeNavigationDestination {
    val route: String

    fun routeWithParsedArgs(args: List<Pair<String, Any>>): String {
        var finalRoute = route

        args.forEach { (key, value) ->
            key.wrappedArg().let { argKey ->
                if (finalRoute.contains(argKey)) {
                    finalRoute = finalRoute.replace(argKey, value.toString())
                }
            }
        }

        return finalRoute
    }
}

@OptIn(ExperimentalMaterialNavigationApi::class, ExperimentalMaterialApi::class)
@Composable
fun <TNavEvent : NavigationEvent> MviBottomSheetNavHost(
    navigationRouter: ComposeNavigationRouter<TNavEvent>,
    startDestination: ComposeNavigationDestination,
    navEvents: Flow<TNavEvent>,
    modifier: Modifier = Modifier,
    onCollapse: () -> Unit = {},
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
) {

    LaunchedEffect(key1 = Unit) {
        navEvents.collectLatest { navigationEvent ->
            navigationRouter.route(navigationEvent)
        }
    }

    val bottomSheetNavigator = rememberBottomSheetNavigator()
    navigationRouter.navController.navigatorProvider.addNavigator(bottomSheetNavigator)

    ModalBottomSheetLayout(
        bottomSheetNavigator = bottomSheetNavigator,
        sheetShape = RoundedCornerShape(
            topEnd = dimensionResource(R.dimen.small_margin),
            topStart = dimensionResource(R.dimen.small_margin)
        )
    ) {
        NavHost(
            navigationRouter.navController,
            remember(route, startDestination.route, builder) {
                navigationRouter.navController.createGraph(startDestination.route, route, builder)
            },
            modifier
        )

        if (bottomSheetNavigator.navigatorSheetState.currentValue != ModalBottomSheetValue.Hidden) {
            DisposableEffect(Unit) {
                onDispose {
                    onCollapse()
                }
            }
        }
    }
}

@Composable
fun <TNavEvent : NavigationEvent> MviFragmentNavHost(
    navigationRouter: ComposeNavigationRouter<TNavEvent>,
    startDestination: ComposeNavigationDestination,
    navEvents: Flow<TNavEvent>,
    modifier: Modifier = Modifier,
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
) {

    LaunchedEffect(key1 = Unit) {
        navEvents.collectLatest { navigationEvent ->
            navigationRouter.route(navigationEvent)
        }
    }

    NavHost(
        navigationRouter.navController,
        remember(route, startDestination.route, builder) {
            navigationRouter.navController.createGraph(startDestination.route, route, builder)
        },
        modifier
    )
}

fun NavGraphBuilder.composable(
    navigationEvent: ComposeNavigationDestination,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit
) {
    addDestination(
        ComposeNavigator.Destination(provider[ComposeNavigator::class], content).apply {
            this.route = navigationEvent.route
            arguments.forEach { (argumentName, argument) ->
                addArgument(argumentName, argument)
            }
            deepLinks.forEach { deepLink ->
                addDeepLink(deepLink)
            }
        }
    )
}

@ExperimentalMaterialNavigationApi
fun NavGraphBuilder.bottomSheet(
    navigationEvent: ComposeNavigationDestination,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable ColumnScope.(backstackEntry: NavBackStackEntry) -> Unit
) {
    addDestination(
        BottomSheetNavigator.Destination(
            provider[BottomSheetNavigator::class],
            content
        ).apply {
            this.route = navigationEvent.route
            arguments.forEach { (argumentName, argument) ->
                addArgument(argumentName, argument)
            }
            deepLinks.forEach { deepLink ->
                addDeepLink(deepLink)
            }
        }
    )
}

fun String.wrappedArg() = "{$this}"
