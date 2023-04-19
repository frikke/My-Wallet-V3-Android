package com.blockchain.betternavigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

@Composable
fun BetterNavHost(
    startDestination: BetterDestination,
    modifier: Modifier = Modifier,
    graph: BetterNavGraph? = null,
    builder: NavGraphBuilder.() -> Unit
) {
    val navController = rememberNavController()
    val argsHolder = rememberArgsHolder()

    LaunchedEffect(Unit) {
        navController.currentBackStackEntryFlow.collectLatest {
            val readableBackStack = navController.backQueue
                .map { it.destination.route ?: "ROOT" }
                .joinToString(" -> ")
            Timber.v("NavBackStack: $readableBackStack")
            val backStackEntriesArgsIds = navController.backQueue.mapNotNull { backStackEntry ->
                val arguments = backStackEntry.arguments
                val argsId = arguments?.getString(BetterDestinationWithArgs.KEY_ARGS_ID)
                argsId
            }

            val idsToRemove = argsHolder.keys - backStackEntriesArgsIds
            idsToRemove.forEach { id ->
                argsHolder.remove(id)
                Timber.e("REMOVING $id")
            }
        }
    }

    CompositionLocalProvider(
        LocalNavigationArgsHolderProvider provides argsHolder,
        LocalNavControllerProvider provides navController
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination.route,
            modifier = modifier,
            route = graph?.route,
            builder = {
                builder()
            },
        )
    }
}
