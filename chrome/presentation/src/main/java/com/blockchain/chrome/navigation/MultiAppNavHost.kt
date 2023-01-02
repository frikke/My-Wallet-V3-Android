package com.blockchain.chrome.navigation

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.blockchain.chrome.composable.MultiAppChrome
import com.blockchain.commonarch.presentation.mvi_v2.compose.NavArgument
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.commonarch.presentation.mvi_v2.compose.navigate
import com.blockchain.commonarch.presentation.mvi_v2.compose.rememberBottomSheetNavigator
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.fiatActions.fiatactions.FiatActionsNavigation
import com.blockchain.home.presentation.navigation.ARG_ACTIVITY_TX_ID
import com.blockchain.home.presentation.navigation.ARG_FIAT_TICKER
import com.blockchain.home.presentation.navigation.ARG_WALLET_MODE
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.home.presentation.navigation.HomeDestination
import com.blockchain.home.presentation.navigation.QrScanNavigation
import com.blockchain.home.presentation.navigation.SettingsNavigation
import com.blockchain.home.presentation.navigation.homeGraph
import com.blockchain.preferences.SuperAppMvpPrefs
import com.blockchain.preferences.WalletModePrefs
import com.blockchain.prices.navigation.PricesNavigation
import com.blockchain.walletmode.WalletMode
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import org.koin.androidx.compose.get

@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
fun MultiAppNavHost(
    prefs: SuperAppMvpPrefs = get(),
    walletModePrefs: WalletModePrefs = get(),
    assetActionsNavigation: AssetActionsNavigation,
    fiatActionsNavigation: FiatActionsNavigation,
    pricesNavigation: PricesNavigation,
    settingsNavigation: SettingsNavigation,
    qrScanNavigation: QrScanNavigation
) {
    val bottomSheetNavigator = rememberBottomSheetNavigator(skipHalfExpanded = true)
    val navController = rememberNavController(bottomSheetNavigator)

    ModalBottomSheetLayout(
        bottomSheetNavigator,
        sheetShape = AppTheme.shapes.large.copy(
            bottomStart = CornerSize(0.dp),
            bottomEnd = CornerSize(0.dp)
        )
    ) {
        NavHost(
            navController = navController,
            startDestination = if (true/*!prefs.hasSeenEducationalWalletMode && !walletModePrefs.userDefaultedToPKW*/) {
                HomeDestination.Introduction
            } else {
                ChromeDestination.Main
            }.route
        ) {
            // main chrome
            chrome(
                navController = navController,
                assetActionsNavigation = assetActionsNavigation,
                settingsNavigation = settingsNavigation,
                pricesNavigation = pricesNavigation,
                qrScanNavigation = qrScanNavigation
            )

            // home screens
            homeGraph(
                launchApp = {
                    navController.navigate(ChromeDestination.Main) {
                        popUpTo(HomeDestination.Introduction.route) {
                            inclusive = true
                        }
                    }
                },
                assetActionsNavigation = assetActionsNavigation,
                onBackPressed = navController::popBackStack
            )
        }
    }
}

private fun NavGraphBuilder.chrome(
    navController: NavHostController,
    assetActionsNavigation: AssetActionsNavigation,
    settingsNavigation: SettingsNavigation,
    pricesNavigation: PricesNavigation,
    qrScanNavigation: QrScanNavigation
) {
    composable(navigationEvent = ChromeDestination.Main) {
        MultiAppChrome(
            onModeLongClicked = { walletMode ->
                navController.navigate(
                    HomeDestination.Introduction,
                    listOf(NavArgument(key = ARG_WALLET_MODE, value = walletMode))
                )
            },
            assetActionsNavigation = assetActionsNavigation,
            settingsNavigation = settingsNavigation,
            pricesNavigation = pricesNavigation,
            qrScanNavigation = qrScanNavigation,
            openCryptoAssets = {
                navController.navigate(HomeDestination.CryptoAssets)
            },
            openActivity = {
                navController.navigate(HomeDestination.Activity)
            },
            openActivityDetail = { txId: String, walletMode: WalletMode ->
                navController.navigate(
                    HomeDestination.ActivityDetail,
                    listOf(
                        NavArgument(key = ARG_ACTIVITY_TX_ID, value = txId),
                        NavArgument(key = ARG_WALLET_MODE, value = walletMode)
                    )
                )
            },
            openReferral = {
                navController.navigate(HomeDestination.Referral)
            },
            openFiatActionDetail = { fiatTicker: String ->
                navController.navigate(
                    HomeDestination.FiatActionDetail,
                    listOf(NavArgument(key = ARG_FIAT_TICKER, fiatTicker))
                )
            },
            openMoreQuickActions = {
                navController.navigate(HomeDestination.MoreQuickActions)
            }
        )
    }
}
