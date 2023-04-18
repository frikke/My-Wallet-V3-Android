package com.blockchain.transactions.swap

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import com.blockchain.chrome.composable.ChromeBottomSheet
import com.blockchain.chrome.composable.ChromeSingleScreen
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationDestination
import com.blockchain.commonarch.presentation.mvi_v2.compose.NavArgument
import com.blockchain.commonarch.presentation.mvi_v2.compose.bottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.commonarch.presentation.mvi_v2.compose.getComposeArgument
import com.blockchain.commonarch.presentation.mvi_v2.compose.navigate
import com.blockchain.commonarch.presentation.mvi_v2.compose.wrappedArg
import com.blockchain.transactions.swap.enteramount.composable.EnterAmount
import com.blockchain.transactions.swap.selectsource.composable.SelectSourceScreen
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
                    openSourceAccounts = {
                        navControllerProvider().navigate(
                            SwapDestination.SourceAccounts
                        )
                    },
                    onBackPressed = onBackPressed
                )
            }
        }

        bottomSheet(navigationEvent = SwapDestination.SourceAccounts) { backStackEntry ->
            ChromeBottomSheet(onClose = onBackPressed) {
                SelectSourceScreen(
                    onAccountSelected = {
                        navControllerProvider().previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("your_key", it)
                        onBackPressed()
                    },
                    onBackPressed = onBackPressed
                )
            }
        }
    }
}

sealed class SwapDestination(
    override val route: String
) : ComposeNavigationDestination {
    object Main : SwapDestination("SwapMain")
    object EnterAmount : SwapDestination("SwapEnterAmount")
    object SourceAccounts : SwapDestination("SwapSourceAccounts")
}
