package com.blockchain.chrome.navigation

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import com.blockchain.home.presentation.navigation.HomeDestination
import com.blockchain.home.presentation.navigation.QrScanNavigation
import com.blockchain.home.presentation.navigation.homeGraph
import com.blockchain.koin.payloadScope
import com.blockchain.nfts.navigation.ARG_ADDRESS
import com.blockchain.nfts.navigation.ARG_NFT_ID
import com.blockchain.nfts.navigation.ARG_PAGE_KEY
import com.blockchain.nfts.navigation.NftDestination
import com.blockchain.nfts.navigation.nftGraph
import com.blockchain.preferences.WalletModePrefs
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.transactions.receive.navigation.receiveGraph
import com.blockchain.walletconnect.ui.navigation.WalletConnectV2Navigation
import com.blockchain.walletconnect.ui.navigation.walletConnectGraph
import com.blockchain.walletmode.WalletMode
import com.dex.presentation.graph.dexGraph
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
fun MultiAppNavHost(
    walletModePrefs: WalletModePrefs = get(),
    walletStatusPrefs: WalletStatusPrefs = get(),
    startPhraseRecovery: () -> Unit,
    showAppRating: () -> Unit,
    assetActionsNavigation: AssetActionsNavigation,
    recurringBuyNavigation: RecurringBuyNavigation,
    settingsNavigation: SettingsNavigation,
    qrScanNavigation: QrScanNavigation,
    supportNavigation: SupportNavigation,
    earnNavigation: EarnNavigation,
    defiBackupNavigation: DefiBackupNavigation,
    openExternalUrl: (url: String) -> Unit,
    processAnnouncementUrl: (url: String) -> Unit,
    openDex: MutableState<Boolean>
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

    // open dex
    LaunchedEffect(openDex.value) {
        if (openDex.value) {
            navController.popBackStack(ChromeDestination.Main.route, inclusive = false)

            multiAppViewModel.onIntent(
                MultiAppIntents.BottomNavigationItemSelected(ChromeBottomNavigationItem.Dex)
            )

            openDex.value = false
        }
    }

    // todo(othman) add chrome pill here to be accessible in single screens also
    // box{sheetlayout; pill}
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
            sheetShape = AppTheme.shapes.veryLarge.topOnly(),
            sheetBackgroundColor = Color.Transparent,
            scrimColor = AppColors.scrim
        ) {
            val popupRoute: String?

            NavHost(
                navController = navController,
                startDestination = when {
                    !walletStatusPrefs.hasSeenCustodialOnboarding && !walletModePrefs.userDefaultedToPKW -> {
                        // has not seen wallets intro && was not defaulted to defi
                        popupRoute = HomeDestination.CustodialIntro.route
                        HomeDestination.CustodialIntro
                    }

                    !walletStatusPrefs.hasSeenDefiOnboarding && walletModePrefs.userDefaultedToPKW -> {
                        // was defaulted to defi && has not seen defi onboarding
                        popupRoute = HomeDestination.DefiIntro.route
                        HomeDestination.DefiIntro
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

                receiveGraph(
                    onBackPressed = navController::popBackStack
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
    earnNavigation: EarnNavigation,
    openExternalUrl: (url: String) -> Unit,
    processAnnouncementUrl: (url: String) -> Unit
) {
    composable(navigationEvent = ChromeDestination.Main) {
        MultiAppChrome(
            viewModel = viewModel,
            onModeLongClicked = { walletMode ->
                navController.navigateToWalletIntro(walletMode)
            },
            startPhraseRecovery = startPhraseRecovery,
            showWalletIntro = { walletMode ->
                navController.navigateToWalletIntro(walletMode)
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
            earnNavigation = earnNavigation,
            processAnnouncementUrl = processAnnouncementUrl,
        )
    }
}

private fun NavHostController.navigateToWalletIntro(
    walletMode: WalletMode
) {
    navigate(
        when (walletMode) {
            WalletMode.CUSTODIAL -> HomeDestination.CustodialIntro
            WalletMode.NON_CUSTODIAL -> HomeDestination.DefiIntro
        },
        args = listOf(NavArgument(ARG_IS_FROM_MODE_SWITCH, true))
    )
}
