package com.blockchain.betternavigation

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.blockchain.commonarch.presentation.mvi_v2.compose.rememberBottomSheetNavigator
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.topOnly
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
fun TypedNavHost(
    startDestination: Destination,
    modifier: Modifier = Modifier,
    graph: NavGraph? = null,
    builder: NavGraphBuilder.() -> Unit
) {
    val bottomSheetNavigator = rememberBottomSheetNavigator(skipHalfExpanded = true)
    val navController = rememberNavController(bottomSheetNavigator)
    val argsHolder = rememberArgsHolder()

    LaunchedEffect(Unit) {
        navController.currentBackStackEntryFlow.collectLatest {
            val readableBackStack = navController.backQueue
                .map { it.destination.route ?: "ROOT" }
                .joinToString(" -> ")
            Timber.v("NavBackStack: $readableBackStack")
            val backStackEntriesArgsIds = navController.backQueue.mapNotNull { backStackEntry ->
                val arguments = backStackEntry.arguments
                val argsId = arguments?.getString(DestinationWithArgs.KEY_ARGS_ID)
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
        LocalNavArgsHolderProvider provides argsHolder,
        LocalNavControllerProvider provides navController
    ) {
        ModalBottomSheetLayout(
            modifier = Modifier.background(AppColors.background),
            bottomSheetNavigator = bottomSheetNavigator,
            sheetShape = AppTheme.shapes.veryLarge.topOnly(),
            sheetBackgroundColor = Color.Transparent,
            scrimColor = AppColors.scrim
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination.route,
                modifier = modifier,
                route = graph?.route,
                builder = {
                    builder()
                }
            )
        }
    }
}
