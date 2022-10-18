package com.blockchain.chrome.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.home.presentation.navigation.HomeDestination
import com.blockchain.home.presentation.navigation.homeGraph
import com.blockchain.chrome.core.composable.MultiAppChrome

@Composable
fun MultiAppNavHost(
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = ChromeDestination.Main.route
    ) {
        chrome(navController)

        homeGraph()
    }
}

private fun NavGraphBuilder.chrome(navController: NavHostController) {
    composable(navigationEvent = ChromeDestination.Main) {
        MultiAppChrome(
            openCryptoAssets = {
                navController.navigate(HomeDestination.CryptoAssets)
            }
        )
    }
}
