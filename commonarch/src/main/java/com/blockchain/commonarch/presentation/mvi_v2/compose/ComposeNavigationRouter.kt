package com.blockchain.commonarch.presentation.mvi_v2.compose

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.createGraph
import androidx.navigation.plusAssign
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.componentlib.R
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator

interface ComposeNavigationRouter : NavigationRouter<ComposeNavigationEvent> {
    var navController: NavHostController
}

/* TODO
    How can we provide this as an interface and keep the "name" parameter while keeping the code clean?
   (by clean I mean -> not forcing implementation classes to override the "name" in the class body.
    Would be cleaner if it could be overridden in the constructor itself)
*/
open class ComposeNavigationEvent(val name: String) : NavigationEvent

@OptIn(ExperimentalMaterialNavigationApi::class, ExperimentalMaterialApi::class)
@Composable
fun MviNavHost(
    navigationRouter: ComposeNavigationRouter,
    startDestination: String,
    modifier: Modifier = Modifier,
    onCollapse: () -> Unit = {},
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
) {
    val bottomSheetNavigator = rememberBottomSheetNavigator()
    navigationRouter.navController = rememberNavController()
    navigationRouter.navController.navigatorProvider += bottomSheetNavigator

    ModalBottomSheetLayout(
        bottomSheetNavigator = bottomSheetNavigator,
        sheetShape = RoundedCornerShape(
            topEnd = dimensionResource(R.dimen.small_margin),
            topStart = dimensionResource(R.dimen.small_margin)
        )
    ) {
        NavHost(
            navigationRouter.navController,
            remember(route, startDestination, builder) {
                navigationRouter.navController.createGraph(startDestination, route, builder)
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
