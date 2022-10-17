package com.blockchain.home.presentation.navigation

import androidx.navigation.NavGraphBuilder
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.home.presentation.allassets.composable.CryptoAssets

fun NavGraphBuilder.homeGraph() {
    composable(navigationEvent = HomeDestination.CryptoAssets) {
        CryptoAssets()
    }

    // add other composable screens here
}
