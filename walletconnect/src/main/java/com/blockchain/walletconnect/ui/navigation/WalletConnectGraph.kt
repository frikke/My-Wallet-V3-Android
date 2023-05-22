package com.blockchain.walletconnect.ui.navigation

import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.blockchain.chrome.composable.ChromeSingleScreen
import com.blockchain.commonarch.presentation.mvi_v2.compose.NavArgument
import com.blockchain.commonarch.presentation.mvi_v2.compose.bottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.commonarch.presentation.mvi_v2.compose.getComposeArgument
import com.blockchain.commonarch.presentation.mvi_v2.compose.navigate
import com.blockchain.walletconnect.ui.composable.WalletConnectDappListScreen
import com.blockchain.walletconnect.ui.composable.WalletConnectDappSessionDetail
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi

@OptIn(ExperimentalMaterialNavigationApi::class)
fun NavGraphBuilder.walletConnectGraph(
    onBackPressed: () -> Unit,
    navController: NavHostController,
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
                }
            )
        }
    }

    bottomSheet(navigationEvent = WalletConnectDestination.WalletConnectManageSession) { backStackEntry ->
        val sessionId = backStackEntry.arguments?.getComposeArgument(WalletConnectDestination.ARG_SESSION_ID)
            .orEmpty()
        val isV2 = backStackEntry.arguments?.getComposeArgument(WalletConnectDestination.ARG_IS_V2_SESSION)
            .orEmpty().toBoolean()

        WalletConnectDappSessionDetail(
            sessionId = sessionId,
            isV2 = isV2,
            onDismiss = {
                navController.popBackStack()
            }
        )
    }
}
