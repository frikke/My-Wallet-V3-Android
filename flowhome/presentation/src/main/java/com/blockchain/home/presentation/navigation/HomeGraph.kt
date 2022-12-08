package com.blockchain.home.presentation.navigation

import androidx.navigation.NavGraphBuilder
import com.blockchain.chrome.composable.MultiAppSingleScreen
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.home.presentation.activity.list.composable.Activity
import com.blockchain.home.presentation.allassets.composable.CryptoAssets
import com.blockchain.home.presentation.referral.composable.ReferralCode

fun NavGraphBuilder.homeGraph(
    assetActionsNavigation: AssetActionsNavigation,
    onBackPressed: () -> Unit
) {
    composable(navigationEvent = HomeDestination.CryptoAssets) {
        MultiAppSingleScreen(
            content = {
                CryptoAssets(
                    assetActionsNavigation = assetActionsNavigation,
                    onBackPressed = onBackPressed
                )
            }
        )
    }

    composable(navigationEvent = HomeDestination.Activity) {
        MultiAppSingleScreen(
            content = {
                Activity(
                    onBackPressed = onBackPressed
                )
            }
        )
    }

    composable(navigationEvent = HomeDestination.Referral) {
        MultiAppSingleScreen(
            content = {
                ReferralCode(
                    onBackPressed = onBackPressed
                )
            }
        )
    }

    // add other composable screens here
}
