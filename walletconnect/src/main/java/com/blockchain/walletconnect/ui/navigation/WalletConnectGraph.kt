package com.blockchain.walletconnect.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.blockchain.chrome.composable.ChromeBottomSheet
import com.blockchain.chrome.composable.ChromeSingleScreen
import com.blockchain.commonarch.presentation.mvi_v2.compose.NavArgument
import com.blockchain.commonarch.presentation.mvi_v2.compose.bottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.commonarch.presentation.mvi_v2.compose.getComposeArgument
import com.blockchain.commonarch.presentation.mvi_v2.compose.navigate
import com.blockchain.walletconnect.ui.composable.WalletConnectAuthRequest
import com.blockchain.walletconnect.ui.composable.WalletConnectDappListScreen
import com.blockchain.walletconnect.ui.composable.WalletConnectDappSessionDetail
import com.blockchain.walletconnect.ui.composable.WalletConnectSessionNotSupported
import com.blockchain.walletconnect.ui.composable.WalletConnectSessionProposal
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi

@OptIn(ExperimentalMaterialNavigationApi::class)
fun NavGraphBuilder.walletConnectGraph(
    navController: NavHostController,
    onBackPressed: () -> Unit,
    onLaunchQrCodeScan: () -> Unit
) {
    composable(navigationEvent = WalletConnectDestination.WalletConnectDappList) {
        ChromeSingleScreen {
            WalletConnectDappListScreen(
                onBackPressed = onBackPressed,
                onSessionClicked = {
                    navController.navigate(
                        WalletConnectDestination.WalletConnectManageSession,
                        listOfNotNull(
                            NavArgument(key = WalletConnectDestination.ARG_SESSION_ID, value = it.sessionId),
                            NavArgument(key = WalletConnectDestination.ARG_IS_V2_SESSION, value = it.isV2)
                        ),
                    )
                },
                onLaunchQrCodeScan = onLaunchQrCodeScan
            )
        }
    }

    bottomSheet(navigationEvent = WalletConnectDestination.WalletConnectManageSession) { backStackEntry ->
        val sessionId = backStackEntry.arguments?.getComposeArgument(WalletConnectDestination.ARG_SESSION_ID)
            .orEmpty()
        val isV2 = backStackEntry.arguments?.getComposeArgument(WalletConnectDestination.ARG_IS_V2_SESSION)
            .orEmpty().toBoolean()

        ChromeBottomSheet(onClose = onBackPressed) {
            WalletConnectDappSessionDetail(
                sessionId = sessionId,
                isV2 = isV2,
                onDismiss = {
                    navController.popBackStack()
                }
            )
        }
    }

    bottomSheet(navigationEvent = WalletConnectDestination.WalletConnectSessionProposal) { backStackEntry ->
        val sessionId = backStackEntry.arguments
            ?.getComposeArgument(WalletConnectDestination.ARG_SESSION_ID).orEmpty()

        val walletAddress = backStackEntry.arguments
            ?.getComposeArgument(WalletConnectDestination.ARG_WALLET_ADDRESS).orEmpty()

        ChromeBottomSheet(onClose = onBackPressed) {
            WalletConnectSessionProposal(
                sessionId = sessionId,
                walletAddress = walletAddress,
                onDismiss = {
                    navController.popBackStack()
                }
            )
        }
    }

    bottomSheet(navigationEvent = WalletConnectDestination.WalletConnectSessionNotSupported) { backStackEntry ->

        val dappName = backStackEntry.arguments
            ?.getComposeArgument(WalletConnectDestination.ARG_DAPP_NAME).orEmpty()
        val dappLogoUrl = backStackEntry.arguments
            ?.getComposeArgument(WalletConnectDestination.ARG_DAPP_LOGO_URL).orEmpty()

        ChromeBottomSheet(onClose = onBackPressed) {
            WalletConnectSessionNotSupported(
                dappName = dappName,
                dappLogoUrl = dappLogoUrl,
                onDismiss = {
                    navController.popBackStack()
                }
            )
        }
    }

    bottomSheet(navigationEvent = WalletConnectDestination.WalletConnectAuthRequest) {
        val authId = it.arguments?.getComposeArgument(WalletConnectDestination.ARG_AUTH_ID).orEmpty()

        ChromeBottomSheet(onClose = onBackPressed) {
            WalletConnectAuthRequest(
                authId = authId,
                onDismiss = { navController.popBackStack() }
            )
        }
    }
}
