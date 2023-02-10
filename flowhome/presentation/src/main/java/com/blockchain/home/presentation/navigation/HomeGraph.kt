package com.blockchain.home.presentation.navigation

import androidx.navigation.NavGraphBuilder
import com.blockchain.chrome.composable.ChromeBottomSheet
import com.blockchain.chrome.composable.ChromeSingleScreen
import com.blockchain.commonarch.presentation.mvi_v2.compose.bottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.commonarch.presentation.mvi_v2.compose.getComposeArgument
import com.blockchain.componentlib.navigation.ModeBackgroundColor
import com.blockchain.home.presentation.activity.detail.composable.ActivityDetail
import com.blockchain.home.presentation.activity.list.composable.Activity
import com.blockchain.home.presentation.allassets.composable.CryptoAssets
import com.blockchain.home.presentation.onboarding.defi.composable.DeFiOnboarding
import com.blockchain.home.presentation.fiat.fundsdetail.composable.FiatFundDetail
import com.blockchain.home.presentation.onboarding.introduction.composable.IntroductionScreens
import com.blockchain.home.presentation.quickactions.MoreActions
import com.blockchain.home.presentation.referral.composable.ReferralCode
import com.blockchain.walletmode.WalletMode
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi

@OptIn(ExperimentalMaterialNavigationApi::class)
fun NavGraphBuilder.homeGraph(
    launchApp: () -> Unit,
    assetActionsNavigation: AssetActionsNavigation,
    onBackPressed: () -> Unit
) {
    composable(navigationEvent = HomeDestination.Introduction) { backStackEntry ->
        val walletMode = backStackEntry.arguments?.getString(ARG_WALLET_MODE)?.run {
            WalletMode.values().firstOrNull { it.name == this }
        }

        ChromeSingleScreen(backgroundColor = ModeBackgroundColor.None) {
            IntroductionScreens(triggeredBy = walletMode, launchApp = launchApp, close = onBackPressed)
        }
    }

    composable(navigationEvent = HomeDestination.DefiOnboarding) { backStackEntry ->
        val isFromModeSwitch = backStackEntry.arguments?.getComposeArgument(ARG_IS_FROM_MODE_SWITCH)
            ?.toBoolean() ?: false

        ChromeSingleScreen(backgroundColor = ModeBackgroundColor.Override(WalletMode.NON_CUSTODIAL)) {
            DeFiOnboarding(
                showCloseIcon = isFromModeSwitch,
                closeOnClick = if (isFromModeSwitch) onBackPressed else launchApp,
                enableDeFiOnClick = if (isFromModeSwitch) onBackPressed else launchApp,
            )
        }
    }

    composable(navigationEvent = HomeDestination.CryptoAssets) {
        ChromeSingleScreen {
            CryptoAssets(
                assetActionsNavigation = assetActionsNavigation,
                onBackPressed = onBackPressed
            )
        }
    }

    composable(navigationEvent = HomeDestination.Activity) {
        ChromeSingleScreen {
            Activity(
                onBackPressed = onBackPressed
            )
        }
    }

    bottomSheet(navigationEvent = HomeDestination.ActivityDetail) { backStackEntry ->
        val txId = backStackEntry.arguments?.getString(ARG_ACTIVITY_TX_ID).orEmpty()
        val walletMode = backStackEntry.arguments?.getString(ARG_WALLET_MODE)?.run {
            WalletMode.values().firstOrNull { it.name == this }
        }

        walletMode?.let {
            ChromeBottomSheet {
                ActivityDetail(
                    selectedTxId = txId,
                    walletMode = walletMode,
                    onCloseClick = onBackPressed
                )
            }
        }
    }

    composable(navigationEvent = HomeDestination.Referral) {
        ChromeSingleScreen {
            ReferralCode(
                onBackPressed = onBackPressed
            )
        }
    }

    bottomSheet(navigationEvent = HomeDestination.FiatActionDetail) { backStackEntry ->
        val fiatTicker = backStackEntry.arguments?.getString(ARG_FIAT_TICKER).orEmpty()
        FiatFundDetail(
            fiatTicker = fiatTicker,
            dismiss = onBackPressed
        )
    }

    bottomSheet(navigationEvent = HomeDestination.MoreQuickActions) { backStackEntry ->
        MoreActions(
            dismiss = onBackPressed,
            assetActionsNavigation = assetActionsNavigation
        )
    }

    // add other composable screens here
}
