package com.blockchain.transactions.swap

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.blockchain.betternavigation.Destination
import com.blockchain.betternavigation.DestinationWithArgs
import com.blockchain.betternavigation.NavGraph
import com.blockchain.betternavigation.TypedNavHost
import com.blockchain.betternavigation.navigateTo
import com.blockchain.betternavigation.navigateUp
import com.blockchain.betternavigation.popUpTo
import com.blockchain.betternavigation.typedBottomSheet
import com.blockchain.betternavigation.typedComposable
import com.blockchain.chrome.composable.ChromeBottomSheet
import com.blockchain.chrome.composable.ChromeSingleScreen
import com.blockchain.transactions.swap.confirmation.composable.ConfirmationArgs
import com.blockchain.transactions.swap.confirmation.composable.ConfirmationScreen
import com.blockchain.transactions.swap.enteramount.composable.EnterAmount
import com.blockchain.transactions.swap.neworderstate.composable.NewOrderStateArgs
import com.blockchain.transactions.swap.neworderstate.composable.NewOrderStateScreen
import com.blockchain.transactions.swap.selectsource.composable.SelectSourceScreen
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi

object SwapGraph : NavGraph() {
    object EnterAmount : Destination()
    object SourceAccounts : Destination()
    object Confirmation : DestinationWithArgs<ConfirmationArgs>()
    object NewOrderState : DestinationWithArgs<NewOrderStateArgs>()
}

@ExperimentalMaterialNavigationApi
fun NavGraphBuilder.swapGraphHost(mainNavController: NavController) {
    // TODO(aromano): navigation TEMP
    composable(SwapGraph::class.java.name) {
        TypedNavHost(
            graph = SwapGraph,
            startDestination = SwapGraph.EnterAmount,
        ) {
            typedComposable(SwapGraph.EnterAmount) {
                ChromeSingleScreen {
                    EnterAmount(
                        navContextProvider = { this },
                        onBackPressed = { navigateUp() }
                    )
                }
            }

            typedBottomSheet(SwapGraph.SourceAccounts) {
                ChromeBottomSheet(onClose = { navigateUp() }) {
                    SelectSourceScreen(
                        navControllerProvider = { this.navController },
                        onBackPressed = { navigateUp() }
                    )
                }
            }

            typedComposable(SwapGraph.Confirmation) { args ->
                ChromeSingleScreen {
                    ConfirmationScreen(
                        args = args,
                        openNewOrderState = { args ->
                            navigateTo(SwapGraph.NewOrderState, args) {
                                popUpTo(SwapGraph)
                            }
                        },
                        backClicked = { navigateUp() },
                    )
                }
            }

            typedComposable(SwapGraph.NewOrderState) { args ->
                ChromeSingleScreen {
                    NewOrderStateScreen(
                        args = args,
                        exitSwap = { mainNavController.navigateUp() },
                    )
                }
            }
        }
    }
}
