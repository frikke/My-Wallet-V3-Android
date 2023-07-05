package com.blockchain.chrome.navigation

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.blockchain.chrome.ChromeBottomNavigationItem
import com.blockchain.chrome.ChromePill
import com.blockchain.chrome.LocalChromePillProvider
import com.blockchain.chrome.LocalNavControllerProvider
import com.blockchain.chrome.MultiAppIntents
import com.blockchain.chrome.MultiAppViewModel
import com.blockchain.chrome.composable.MultiAppChrome
import com.blockchain.commonarch.presentation.mvi_v2.compose.NavArgument
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.commonarch.presentation.mvi_v2.compose.navigate
import com.blockchain.commonarch.presentation.mvi_v2.compose.rememberBottomSheetNavigator
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SystemColors
import com.blockchain.componentlib.theme.topOnly
import com.blockchain.earn.navigation.EarnNavigation
import com.blockchain.home.presentation.navigation.ARG_IS_FROM_MODE_SWITCH
import com.blockchain.home.presentation.navigation.ARG_RECURRING_BUY_ID
import com.blockchain.home.presentation.navigation.ARG_WALLET_MODE
import com.blockchain.home.presentation.navigation.HomeDestination
import com.blockchain.home.presentation.navigation.QrScanNavigation
import com.blockchain.home.presentation.navigation.homeGraph
import com.blockchain.koin.payloadScope
import com.blockchain.nfts.navigation.ARG_ADDRESS
import com.blockchain.nfts.navigation.ARG_NFT_ID
import com.blockchain.nfts.navigation.ARG_PAGE_KEY
import com.blockchain.nfts.navigation.NftDestination
import com.blockchain.nfts.navigation.NftNavigation
import com.blockchain.nfts.navigation.nftGraph
import com.blockchain.preferences.SuperAppMvpPrefs
import com.blockchain.preferences.WalletModePrefs
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.walletconnect.ui.navigation.WalletConnectV2Navigation
import com.blockchain.walletconnect.ui.navigation.walletConnectGraph
import com.dex.presentation.graph.dexGraph
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
fun MultiAppNavHost(
    superAppMvpPrefs: SuperAppMvpPrefs = get(),
    walletModePrefs: WalletModePrefs = get(),
    walletStatusPrefs: WalletStatusPrefs = get(),
    startPhraseRecovery: () -> Unit,
    showAppRating: () -> Unit,
    assetActionsNavigation: AssetActionsNavigation,
    recurringBuyNavigation: RecurringBuyNavigation,
    settingsNavigation: SettingsNavigation,
    qrScanNavigation: QrScanNavigation,
    supportNavigation: SupportNavigation,
    nftNavigation: NftNavigation,
    earnNavigation: EarnNavigation,
    defiBackupNavigation: DefiBackupNavigation,
    openExternalUrl: (url: String) -> Unit,
    processAnnouncementUrl: (url: String) -> Unit
) {
    val multiAppViewModel: MultiAppViewModel = getViewModel(scope = payloadScope)

    val bottomSheetNavigator = rememberBottomSheetNavigator(skipHalfExpanded = true)
    val navController = rememberNavController(bottomSheetNavigator)

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val walletConnectV2Navigation: WalletConnectV2Navigation = get(
        scope = payloadScope,
        parameters = { parametersOf(lifecycle, navController) }
    )

    // update system colors to light/dark
    SystemColors()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                walletConnectV2Navigation.launchWalletConnectV2()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val chromePill: ChromePill = get(scope = payloadScope)
    CompositionLocalProvider(
        LocalChromePillProvider provides chromePill,
        LocalNavControllerProvider provides navController,
        LocalAssetActionsNavigationProvider provides assetActionsNavigation,
        LocalSettingsNavigationProvider provides settingsNavigation,
        LocalDefiBackupNavigationProvider provides defiBackupNavigation,
        LocalRecurringBuyNavigationProvider provides recurringBuyNavigation,
        LocalSupportNavigationProvider provides supportNavigation,
    ) {
        ModalBottomSheetLayout(
            modifier = Modifier.background(AppColors.background),
            bottomSheetNavigator = bottomSheetNavigator,
            sheetShape = AppTheme.shapes.veryLarge.topOnly()
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
                    viewModel = multiAppViewModel,
                    navController = navController,
                    startPhraseRecovery = startPhraseRecovery,
                    qrScanNavigation = qrScanNavigation,
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
                    openRecurringBuyDetail = { recurringBuyId ->
                        navController.navigate(
                            destination = HomeDestination.RecurringBuyDetail,
                            args = listOf(
                                NavArgument(key = ARG_RECURRING_BUY_ID, value = recurringBuyId)
                            )
                        )
                    },
                    assetActionsNavigation = assetActionsNavigation,
                    onBackPressed = navController::popBackStack,
                    openDex = {
                        multiAppViewModel.onIntent(
                            MultiAppIntents.BottomNavigationItemSelected(ChromeBottomNavigationItem.Dex)
                        )
                    }
                )

                nftGraph(
                    openExternalUrl = openExternalUrl,
                    onBackPressed = navController::popBackStack
                )

                dexGraph(
                    onBackPressed = navController::popBackStack,
                    navController = navController
                )

                walletConnectGraph(
                    onBackPressed = navController::popBackStack,
                    navController = navController,
                    onLaunchQrCodeScan = { qrScanNavigation.launchQrScan() },
                )
            }
        }
    }
}

private fun NavGraphBuilder.chrome(
    viewModel: MultiAppViewModel,
    navController: NavHostController,
    startPhraseRecovery: () -> Unit,
    showAppRating: () -> Unit,
    qrScanNavigation: QrScanNavigation,
    nftNavigation: NftNavigation,
    earnNavigation: EarnNavigation,
    openExternalUrl: (url: String) -> Unit,
    processAnnouncementUrl: (url: String) -> Unit
) {
    composable(navigationEvent = ChromeDestination.Main) {
        MultiAppChrome(
            viewModel = viewModel,
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
            qrScanNavigation = qrScanNavigation,
            graphNavController = navController,
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
            processAnnouncementUrl = processAnnouncementUrl,
        )
    }
}
