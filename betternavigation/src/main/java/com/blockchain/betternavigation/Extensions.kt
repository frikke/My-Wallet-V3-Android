package com.blockchain.betternavigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.get
import androidx.navigation.navArgument
import androidx.navigation.navigation
import java.io.Serializable

fun NavGraphBuilder.betterNavGraph(
    startDestination: BetterDestination,
    graph: BetterNavGraph,
    builder: NavGraphBuilder.() -> Unit
) {
    navigation(
        startDestination.route,
        graph.route,
        builder,
    )
}

// TODO(aromano): Implement
// fun <Args : Serializable?> NavGraphBuilder.betterNavGraph(
//    startDestination: BetterDestinationWithArgs<Args>,
//    startingArgs: Args,
//    route: BetterNavGraph,
//    builder: NavGraphBuilder.() -> Unit
// ) {
//    navigation(
//        startDestination.route,
//        route.graphRoute,
//        builder,
//    )
// }

fun NavGraphBuilder.betterDestination(
    destination: BetterDestination,
    content: @Composable BetterNavigationContext.() -> Unit
) {
    addDestination(
        ComposeNavigator.Destination(
            navigator = provider[ComposeNavigator::class],
            content = {
                val navController = LocalNavControllerProvider.current
                val argsHolder = LocalNavigationArgsHolderProvider.current
                val navigationContext = BetterNavigationContext(navController, argsHolder)
                content(navigationContext)
            }
        ).apply {
            this.route = destination.route
        }
    )
}

fun <Args : Serializable?> NavGraphBuilder.betterDestination(
    destination: BetterDestinationWithArgs<Args>,
    content: @Composable BetterNavigationContext.(Args) -> Unit
) {
    addDestination(
        ComposeNavigator.Destination(
            navigator = provider[ComposeNavigator::class],
            content = { backStackEntry ->
                val arguments = backStackEntry.arguments!!
                val argsId = arguments.getString(BetterDestinationWithArgs.KEY_ARGS_ID)!!
                val navController = LocalNavControllerProvider.current
                val argsHolder = LocalNavigationArgsHolderProvider.current
                val args = remember(argsId) {
                    @Suppress("UNCHECKED_CAST")
                    argsHolder[argsId] as Args
                }
                val navigationContext = BetterNavigationContext(navController, argsHolder)
                content(navigationContext, args)
            }
        ).apply {
            this.route = destination.route
            val argument = navArgument(BetterDestinationWithArgs.KEY_ARGS_ID) {
                type = NavType.StringType
            }
            addArgument(argument.name, argument.argument)
        }
    )
}
