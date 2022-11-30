package com.blockchain.home.presentation.navigation

import androidx.navigation.NavGraphBuilder
import com.blockchain.chrome.composable.MultiAppSingleScreen
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.home.presentation.activity.list.composable.Activity
import com.blockchain.home.presentation.allassets.composable.CryptoAssets
import com.blockchain.home.presentation.referral.composable.ReferralCode

fun NavGraphBuilder.homeGraph(
    onBackPressed: () -> Unit
) {
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
                Activity()
            }
        )
    }

    composable(navigationEvent = HomeDestination.Referral) {
        MultiAppSingleScreen(
            content = {
                ReferralCode(onBackPressed = onBackPressed)
            }
        )
    }

    // add other composable screens here
}
