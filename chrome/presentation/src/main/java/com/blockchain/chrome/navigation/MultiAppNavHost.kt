package com.blockchain.chrome.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.blockchain.chrome.composable.MultiAppChrome
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.commonarch.presentation.mvi_v2.compose.navigate
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.home.presentation.navigation.HomeDestination
import com.blockchain.home.presentation.navigation.homeGraph
import com.blockchain.prices.navigation.PricesNavigation

@Composable
fun MultiAppNavHost(
    navController: NavHostController,
    assetActionsNavigation: AssetActionsNavigation,
    pricesNavigation: PricesNavigation
) {
    NavHost(
        navController = navController,
        startDestination = ChromeDestination.Main.route
    ) {
        // main chrome
        chrome(
            navController = navController,
            assetActionsNavigation = assetActionsNavigation,
            pricesNavigation = pricesNavigation
        )

        // home screens
        homeGraph(
            assetActionsNavigation = assetActionsNavigation,
            onBackPressed = navController::popBackStack
        )
    }
}

private fun NavGraphBuilder.chrome(
    navController: NavHostController,
    assetActionsNavigation: AssetActionsNavigation,
    pricesNavigation: PricesNavigation
) {
    composable(navigationEvent = ChromeDestination.Main) {
        MultiAppChrome(
            assetActionsNavigation = assetActionsNavigation,
            pricesNavigation = pricesNavigation,
            openCryptoAssets = {
                navController.navigate(HomeDestination.CryptoAssets)
            },
            openActivity = {
                navController.navigate(HomeDestination.Activity)
            },
            openReferral = {
                navController.navigate(HomeDestination.Referral)
            }
        )
    }
}
