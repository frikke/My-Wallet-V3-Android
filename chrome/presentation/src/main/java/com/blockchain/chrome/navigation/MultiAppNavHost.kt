package com.blockchain.chrome.navigation

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.blockchain.chrome.ChromeBottomNavigationItem
import com.blockchain.chrome.composable.MultiAppChrome
import com.blockchain.commonarch.presentation.mvi_v2.compose.NavArgument
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.commonarch.presentation.mvi_v2.compose.navigate
import com.blockchain.commonarch.presentation.mvi_v2.compose.rememberBottomSheetNavigator
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.earn.navigation.EarnNavigation
import com.blockchain.home.presentation.navigation.ARG_ACTIVITY_TX_ID
import com.blockchain.home.presentation.navigation.ARG_FIAT_TICKER
import com.blockchain.home.presentation.navigation.ARG_IS_FROM_MODE_SWITCH
import com.blockchain.home.presentation.navigation.ARG_WALLET_MODE
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.home.presentation.navigation.HomeDestination
import com.blockchain.home.presentation.navigation.QrScanNavigation
import com.blockchain.home.presentation.navigation.SettingsNavigation
import com.blockchain.home.presentation.navigation.SupportNavigation
import com.blockchain.home.presentation.navigation.homeGraph
import com.blockchain.nfts.navigation.ARG_ADDRESS
import com.blockchain.nfts.navigation.ARG_NFT_ID
import com.blockchain.nfts.navigation.ARG_PAGE_KEY
import com.blockchain.nfts.navigation.NftDestination
import com.blockchain.nfts.navigation.NftNavigation
import com.blockchain.nfts.navigation.nftGraph
import com.blockchain.preferences.SuperAppMvpPrefs
import com.blockchain.preferences.WalletModePrefs
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.prices.navigation.PricesNavigation
import com.blockchain.walletmode.WalletMode
import com.dex.presentation.graph.dexGraph
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import org.koin.androidx.compose.get

@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
fun MultiAppNavHost(
    superAppMvpPrefs: SuperAppMvpPrefs = get(),
    walletModePrefs: WalletModePrefs = get(),
    walletStatusPrefs: WalletStatusPrefs = get(),
    startPhraseRecovery: () -> Unit,
    showAppRating: () -> Unit,
    assetActionsNavigation: AssetActionsNavigation,
    pricesNavigation: PricesNavigation,
    settingsNavigation: SettingsNavigation,
    qrScanNavigation: QrScanNavigation,
    supportNavigation: SupportNavigation,
    nftNavigation: NftNavigation,
    earnNavigation: EarnNavigation,
    openExternalUrl: (url: String) -> Unit,
    processAnnouncementUrl: (url: String) -> Unit
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
        val popupRoute: String?

        NavHost(
            navController = navController,
            startDestination = when {
                !superAppMvpPrefs.hasSeenEducationalWalletMode && !walletModePrefs.userDefaultedToPKW -> {
                    // has not seen wallets intro && was not defaulted to defi
                    popupRoute = HomeDestination.Introduction.route
                    HomeDestination.Introduction
                }

                !walletStatusPrefs.hasSeenDefiOnboarding && walletModePrefs.userDefaultedToPKW -> {
                    // was defaulted to defi && has not seen defi onboarding
                    popupRoute = HomeDestination.DefiOnboarding.route
                    HomeDestination.DefiOnboarding
                }

                else -> {
                    popupRoute = null
                    ChromeDestination.Main
                }
            }.route
        ) {
            // main chrome
            chrome(
                navController = navController,
                startPhraseRecovery = startPhraseRecovery,
                assetActionsNavigation = assetActionsNavigation,
                settingsNavigation = settingsNavigation,
                pricesNavigation = pricesNavigation,
                qrScanNavigation = qrScanNavigation,
                supportNavigation = supportNavigation,
                showAppRating = showAppRating,
                openExternalUrl = openExternalUrl,
                nftNavigation = nftNavigation,
                earnNavigation = earnNavigation,
                processAnnouncementUrl = processAnnouncementUrl
            )

            // home screens
            homeGraph(
                launchApp = {
                    navController.navigate(ChromeDestination.Main) {
                        check(popupRoute != null)

                        popUpTo(popupRoute) {
                            inclusive = true
                        }
                    }
                },
                assetActionsNavigation = assetActionsNavigation,
                onBackPressed = navController::popBackStack,
                openDex = {
                    navController.navigate(ChromeBottomNavigationItem.Dex.route)
                }
            )

            nftGraph(
                openExternalUrl = openExternalUrl,
                onBackPressed = navController::popBackStack
            )
            dexGraph(
                onBackPressed = navController::popBackStack
            )
        }
    }
}

private fun NavGraphBuilder.chrome(
    navController: NavHostController,
    startPhraseRecovery: () -> Unit,
    showAppRating: () -> Unit,
    assetActionsNavigation: AssetActionsNavigation,
    settingsNavigation: SettingsNavigation,
    pricesNavigation: PricesNavigation,
    qrScanNavigation: QrScanNavigation,
    supportNavigation: SupportNavigation,
    nftNavigation: NftNavigation,
    earnNavigation: EarnNavigation,
    openExternalUrl: (url: String) -> Unit,
    processAnnouncementUrl: (url: String) -> Unit
) {
    composable(navigationEvent = ChromeDestination.Main) {
        MultiAppChrome(
            onModeLongClicked = { walletMode ->
                navController.navigate(
                    HomeDestination.Introduction,
                    listOf(NavArgument(key = ARG_WALLET_MODE, value = walletMode))
                )
            },
            startPhraseRecovery = startPhraseRecovery,
            showDefiOnboarding = {
                navController.navigate(
                    destination = HomeDestination.DefiOnboarding,
                    args = listOf(NavArgument(ARG_IS_FROM_MODE_SWITCH, true))
                )
            },
            assetActionsNavigation = assetActionsNavigation,
            settingsNavigation = settingsNavigation,
            pricesNavigation = pricesNavigation,
            qrScanNavigation = qrScanNavigation,
            supportNavigation = supportNavigation,
            openCryptoAssets = {
                navController.navigate(HomeDestination.CryptoAssets)
            },
            openRecurringBuys = {
                navController.navigate(HomeDestination.RecurringBuys)
            },
            openActivity = {
                navController.navigate(HomeDestination.Activity)
            },
            openActivityDetail = { txId: String, walletMode: WalletMode ->
                navController.navigate(
                    destination = HomeDestination.ActivityDetail,
                    args = listOf(
                        NavArgument(key = ARG_ACTIVITY_TX_ID, value = txId),
                        NavArgument(key = ARG_WALLET_MODE, value = walletMode)
                    )
                )
            },
            openReferral = {
                navController.navigate(HomeDestination.Referral)
            },
            graphNavController = navController,
            openSwapDexOption = {
                navController.navigate(HomeDestination.SwapDexOptions)
            },
            openFiatActionDetail = { fiatTicker: String ->
                navController.navigate(
                    HomeDestination.FiatActionDetail,
                    listOf(NavArgument(key = ARG_FIAT_TICKER, fiatTicker))
                )
            },
            openMoreQuickActions = {
                navController.navigate(HomeDestination.MoreQuickActions)
            },
            showAppRating = showAppRating,
            openExternalUrl = openExternalUrl,
            openNftHelp = {
                navController.navigate(NftDestination.Help)
            },
            openNftDetail = { nftId, address, pageKey ->
                navController.navigate(
                    NftDestination.Detail,
                    listOfNotNull(
                        NavArgument(key = ARG_NFT_ID, value = nftId),
                        NavArgument(key = ARG_ADDRESS, value = address),
                        pageKey?.let { NavArgument(key = ARG_PAGE_KEY, value = pageKey) }
                    )
                )
            },
            nftNavigation = nftNavigation,
            earnNavigation = earnNavigation,
            processAnnouncementUrl = processAnnouncementUrl
        )
    }
}
