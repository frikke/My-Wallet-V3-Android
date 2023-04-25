package com.blockchain.betternavigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.get
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import java.io.Serializable

fun NavGraphBuilder.navGraph(
    startDestination: Destination,
    graph: NavGraph,
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

fun NavGraphBuilder.typedComposable(
    destination: Destination,
    content: @Composable NavContext.() -> Unit
) {
    addDestination(
        ComposeNavigator.Destination(
            navigator = provider[ComposeNavigator::class],
            content = {
                val navController = LocalNavControllerProvider.current
                val argsHolder = LocalNavArgsHolderProvider.current
                val navContext = NavContext(navController, argsHolder)
                content(navContext)
            }
        ).apply {
            this.route = destination.route
        }
    )
}

fun <Args : Serializable?> NavGraphBuilder.typedComposable(
    destination: DestinationWithArgs<Args>,
    content: @Composable NavContext.(Args) -> Unit
) {
    addDestination(
        ComposeNavigator.Destination(
            navigator = provider[ComposeNavigator::class],
            content = { backStackEntry ->
                val arguments = backStackEntry.arguments!!
                val argsId = arguments.getString(DestinationWithArgs.KEY_ARGS_ID)!!
                val navController = LocalNavControllerProvider.current
                val argsHolder = LocalNavArgsHolderProvider.current
                val args = remember(argsId) {
                    @Suppress("UNCHECKED_CAST")
                    argsHolder[argsId] as Args
                }
                val navContext = NavContext(navController, argsHolder)
                content(navContext, args)
            }
        ).apply {
            this.route = destination.route
            val argument = navArgument(DestinationWithArgs.KEY_ARGS_ID) {
                type = NavType.StringType
            }
            addArgument(argument.name, argument.argument)
        }
    )
}

@OptIn(ExperimentalMaterialNavigationApi::class)
fun NavGraphBuilder.typedBottomSheet(
    destination: Destination,
    content: @Composable NavContext.() -> Unit
) {
    addDestination(
        BottomSheetNavigator.Destination(
            navigator = provider[BottomSheetNavigator::class],
            content = {
                val navController = LocalNavControllerProvider.current
                val argsHolder = LocalNavArgsHolderProvider.current
                val navContext = NavContext(navController, argsHolder)
                content(navContext)
            }
        ).apply {
            this.route = destination.route
        }
    )
}

@OptIn(ExperimentalMaterialNavigationApi::class)
fun <Args : Serializable?> NavGraphBuilder.typedBottomSheet(
    destination: DestinationWithArgs<Args>,
    content: @Composable NavContext.(Args) -> Unit
) {
    addDestination(
        BottomSheetNavigator.Destination(
            navigator = provider[BottomSheetNavigator::class],
            content = { backStackEntry ->
                val arguments = backStackEntry.arguments!!
                val argsId = arguments.getString(DestinationWithArgs.KEY_ARGS_ID)!!
                val navController = LocalNavControllerProvider.current
                val argsHolder = LocalNavArgsHolderProvider.current
                val args = remember(argsId) {
                    argsHolder[argsId] as Args
                }
                val navContext = NavContext(navController, argsHolder)
                content(navContext, args)
            }
        ).apply {
            this.route = destination.route
            val argument = navArgument(DestinationWithArgs.KEY_ARGS_ID) {
                type = NavType.StringType
            }
            addArgument(argument.name, argument.argument)
        }
    )
}
