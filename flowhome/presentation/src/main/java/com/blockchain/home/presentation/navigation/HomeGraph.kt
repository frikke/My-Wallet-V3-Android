package com.blockchain.home.presentation.navigation

import androidx.navigation.NavGraphBuilder
import com.blockchain.chrome.composable.MultiAppSingleScreen
import com.blockchain.commonarch.presentation.mvi_v2.compose.bottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.home.presentation.activity.list.composable.Activity
import com.blockchain.home.presentation.allassets.composable.CryptoAssets
import com.blockchain.home.presentation.fiat.fundsdetail.composable.FiatFundDetail
import com.blockchain.home.presentation.referral.composable.ReferralCode
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import info.blockchain.balance.FiatCurrency

@OptIn(ExperimentalMaterialNavigationApi::class) fun NavGraphBuilder.homeGraph(
    assetActionsNavigation: AssetActionsNavigation,
    onBackPressed: () -> Unit
) {
    composable(navigationEvent = HomeDestination.CryptoAssets) {
        MultiAppSingleScreen {
            CryptoAssets(
                assetActionsNavigation = assetActionsNavigation,
                onBackPressed = onBackPressed
            )
        }
    }

    composable(navigationEvent = HomeDestination.Activity) {
        MultiAppSingleScreen {
            Activity(
                onBackPressed = onBackPressed
            )
        }
    }

    composable(navigationEvent = HomeDestination.Referral) {
        MultiAppSingleScreen {
            ReferralCode(
                onBackPressed = onBackPressed
            )
        }
    }

    bottomSheet(navigationEvent = HomeDestination.FiatActionDetail) { backStackEntry ->
        val currency = backStackEntry.arguments?.getSerializable("fiatCurrency") as FiatCurrency
        FiatFundDetail(
            currency = currency,
            onBackPressed = onBackPressed
        )
    }

    // add other composable screens here
}
