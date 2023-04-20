package com.blockchain.transactions.swap

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import com.blockchain.chrome.composable.ChromeBottomSheet
import com.blockchain.chrome.composable.ChromeSingleScreen
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationDestination
import com.blockchain.commonarch.presentation.mvi_v2.compose.bottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.commonarch.presentation.mvi_v2.compose.getComposeArgument
import com.blockchain.commonarch.presentation.mvi_v2.compose.wrappedArg
import com.blockchain.transactions.swap.enteramount.composable.EnterAmount
import com.blockchain.transactions.swap.selectsource.composable.SelectSourceScreen
import com.blockchain.transactions.swap.selecttarget.composable.SelectTargetScreen
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi

@ExperimentalMaterialNavigationApi
fun NavGraphBuilder.swapGraph(
    navControllerProvider: () -> NavHostController,
    onBackPressed: () -> Unit
) {
    navigation(startDestination = SwapDestination.EnterAmount.route, route = SwapDestination.Main.route) {
        composable(navigationEvent = SwapDestination.EnterAmount) {
            ChromeSingleScreen {
                EnterAmount(
                    navControllerProvider = navControllerProvider,
                    onBackPressed = onBackPressed
                )
            }
        }

        bottomSheet(navigationEvent = SwapDestination.SourceAccounts) {
            ChromeBottomSheet(onClose = onBackPressed) {
                SelectSourceScreen(
                    navControllerProvider = navControllerProvider,
                    onBackPressed = onBackPressed
                )
            }
        }

        bottomSheet(navigationEvent = SwapDestination.TargetAccounts) { backStackEntry ->
            val sourceTicker = backStackEntry.arguments?.getComposeArgument(ARG_SOURCE_ACCOUNT_TICKER).orEmpty()
            ChromeBottomSheet(onClose = onBackPressed) {
                SelectTargetScreen(
                    sourceTicker = sourceTicker,
                    navControllerProvider = navControllerProvider,
                    onBackPressed = onBackPressed
                )
            }
        }
    }
}

const val ARG_SOURCE_ACCOUNT_TICKER = "ARG_SOURCE_ACCOUNT_TICKER"

sealed class SwapDestination(
    override val route: String
) : ComposeNavigationDestination {
    object Main : SwapDestination("SwapMain")
    object EnterAmount : SwapDestination("SwapEnterAmount")
    object SourceAccounts : SwapDestination("SwapSourceAccounts")
    object TargetAccounts : SwapDestination("SwapTargetAccounts/${ARG_SOURCE_ACCOUNT_TICKER.wrappedArg()}")
}
