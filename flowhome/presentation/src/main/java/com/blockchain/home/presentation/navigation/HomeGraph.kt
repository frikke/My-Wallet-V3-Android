package com.blockchain.home.presentation.navigation

import androidx.navigation.NavGraphBuilder
import com.blockchain.chrome.composable.MultiAppSingleScreen
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.home.presentation.activity.list.composable.Acitivity
import com.blockchain.home.presentation.allassets.composable.CryptoAssets

fun NavGraphBuilder.homeGraph() {
    composable(navigationEvent = HomeDestination.CryptoAssets) {
        MultiAppSingleScreen(
            content = {
                CryptoAssets()
            }
        )
    }

    composable(navigationEvent = HomeDestination.Activity) {
        MultiAppSingleScreen(
            content = {
                Acitivity()
            }
        )
    }

    // add other composable screens here
}
