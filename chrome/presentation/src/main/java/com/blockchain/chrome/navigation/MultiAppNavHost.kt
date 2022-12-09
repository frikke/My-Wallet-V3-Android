package com.blockchain.chrome.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.blockchain.chrome.composable.MultiAppChrome
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.commonarch.presentation.mvi_v2.compose.navigate
import com.blockchain.commonarch.presentation.mvi_v2.compose.rememberBottomSheetNavigator
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.home.presentation.navigation.HomeDestination
import com.blockchain.home.presentation.navigation.homeGraph
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout

@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
fun MultiAppNavHost(
    navController: NavHostController,
    assetActionsNavigation: AssetActionsNavigation,
) {
    val bottomSheetNavigator = rememberBottomSheetNavigator(skipHalfExpanded = true)
    val navController = rememberNavController(bottomSheetNavigator)

    ModalBottomSheetLayout(bottomSheetNavigator) {
        NavHost(
            navController = navController,
            startDestination = ChromeDestination.Main.route
        ) {
            // main chrome
            chrome(
                navController = navController,
                assetActionsNavigation = assetActionsNavigation
            )

            // home screens
            homeGraph(
                assetActionsNavigation = assetActionsNavigation,
                onBackPressed = navController::popBackStack
            )
        }
    }
}

private fun NavGraphBuilder.chrome(navController: NavHostController, assetActionsNavigation: AssetActionsNavigation) {
    composable(navigationEvent = ChromeDestination.Main) {
        MultiAppChrome(
            assetActionsNavigation = assetActionsNavigation,
            openCryptoAssets = {
                navController.navigate(HomeDestination.CryptoAssets)
            },
            openActivity = {
                navController.navigate(HomeDestination.Activity)
            },
            openReferral = {
                navController.navigate(HomeDestination.Referral)
            },
            openFiatActionDetail = {
                navController.navigate(HomeDestination.FiatActionDetail)
            }
        )
    }
}
