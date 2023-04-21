package com.blockchain.transactions.swap

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.blockchain.betternavigation.BetterDestination
import com.blockchain.betternavigation.BetterDestinationWithArgs
import com.blockchain.betternavigation.BetterNavGraph
import com.blockchain.betternavigation.BetterNavHost
import com.blockchain.betternavigation.betterDestination
import com.blockchain.betternavigation.betterSheetDestination
import com.blockchain.betternavigation.navigateUp
import com.blockchain.chrome.composable.ChromeBottomSheet
import com.blockchain.chrome.composable.ChromeSingleScreen
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationDestination
import com.blockchain.commonarch.presentation.mvi_v2.compose.bottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.transactions.swap.confirmation.composable.ConfirmationArgs
import com.blockchain.transactions.swap.confirmation.composable.ConfirmationScreen
import com.blockchain.transactions.swap.enteramount.composable.EnterAmount
import com.blockchain.transactions.swap.selectsource.composable.SelectSourceScreen
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi

object SwapGraph : BetterNavGraph() {
    object EnterAmount : BetterDestination()
    object SourceAccounts : BetterDestination()
    object Confirmation : BetterDestinationWithArgs<ConfirmationArgs>()
}

@ExperimentalMaterialNavigationApi
fun NavGraphBuilder.swapGraphHost() {
    // TODO(aromano): navigation TEMP
    composable(SwapGraph::class.java.name) {
        BetterNavHost(
            startDestination = SwapGraph.EnterAmount,
        ) {
            betterDestination(SwapGraph.EnterAmount) {
                ChromeSingleScreen {
                    EnterAmount(
                        navContextProvider = { this },
                        onBackPressed = { navigateUp() }
                    )
                }
            }

            betterSheetDestination(SwapGraph.SourceAccounts) {
                ChromeBottomSheet(onClose = { navigateUp() }) {
                    SelectSourceScreen(
                        navControllerProvider = { this.navController },
                        onBackPressed = { navigateUp() }
                    )
                }
            }

            betterDestination(SwapGraph.Confirmation) { args ->
                ChromeSingleScreen {
                    ConfirmationScreen(
                        args = args,
                    )
                }
            }
        }
    }
}
