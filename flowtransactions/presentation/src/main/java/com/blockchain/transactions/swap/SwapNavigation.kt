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
import com.blockchain.betternavigation.popBackStack
import com.blockchain.betternavigation.popUpTo
import com.blockchain.betternavigation.typedBottomSheet
import com.blockchain.betternavigation.typedComposable
import com.blockchain.chrome.composable.ChromeBottomSheet
import com.blockchain.chrome.composable.ChromeSingleScreen
import com.blockchain.koin.payloadScope
import com.blockchain.transactions.swap.confirmation.composable.ConfirmationArgs
import com.blockchain.transactions.swap.confirmation.composable.ConfirmationScreen
import com.blockchain.transactions.swap.enteramount.EnterAmountIntent
import com.blockchain.transactions.swap.enteramount.EnterAmountViewModel
import com.blockchain.transactions.swap.enteramount.composable.EnterAmount
import com.blockchain.transactions.swap.neworderstate.composable.NewOrderStateArgs
import com.blockchain.transactions.swap.neworderstate.composable.NewOrderStateScreen
import com.blockchain.transactions.swap.selectsource.composable.SelectSourceScreen
import com.blockchain.transactions.swap.selecttarget.composable.SelectTargetAsset
import com.blockchain.transactions.swap.selecttargetaccount.composable.SelectTargetAccount
import com.blockchain.transactions.swap.selecttargetaccount.composable.SelectTargetAccountArgs
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import org.koin.androidx.compose.getViewModel

object SwapGraph : NavGraph() {
    object EnterAmount : Destination()
    object SourceAccounts : Destination()
    object SelectTarget : DestinationWithArgs<String>()
    object TargetAsset : DestinationWithArgs<String>()
    object TargetAccount : DestinationWithArgs<SelectTargetAccountArgs>()
    object Confirmation : DestinationWithArgs<ConfirmationArgs>()
    object NewOrderState : DestinationWithArgs<NewOrderStateArgs>()
}

@ExperimentalMaterialNavigationApi
fun NavGraphBuilder.swapGraphHost(mainNavController: NavController) {
    // TODO(aromano): navigation TEMP
    composable(SwapGraph::class.java.name) {
        val viewModel: EnterAmountViewModel = getViewModel(scope = payloadScope)

        TypedNavHost(
            graph = SwapGraph,
            startDestination = SwapGraph.EnterAmount,
        ) {
            typedComposable(SwapGraph.EnterAmount) {
                ChromeSingleScreen {
                    EnterAmount(
                        viewModel = viewModel,
                        navContextProvider = { this },
                        onBackPressed = ::navigateUp
                    )
                }
            }

            typedBottomSheet(SwapGraph.SourceAccounts) {
                ChromeBottomSheet(onClose = ::navigateUp) {
                    SelectSourceScreen(
                        accountSelected = {
                            viewModel.onIntent(EnterAmountIntent.FromAccountChanged(it))
                        },
                        onBackPressed = ::navigateUp
                    )
                }
            }

            // support nested graph navigation(...)
            typedBottomSheet(SwapGraph.TargetAsset) { sourceTicker ->
                ChromeBottomSheet(onClose = ::navigateUp) {
                    SelectTargetAsset(
                        sourceTicker = sourceTicker,
                        accountSelected = {
                            viewModel.onIntent(EnterAmountIntent.ToAccountChanged(it))
                        },
                        navContextProvider = { this },
                        onClosePressed = ::navigateUp
                    )
                }
            }

            typedBottomSheet(SwapGraph.TargetAccount) { args ->
                ChromeBottomSheet(onClose = ::navigateUp) {
                    SelectTargetAccount(
                        sourceTicker = args.sourceTicker,
                        targetTicker = args.targetTicker,
                        mode = args.mode,
                        accountSelected = {
                            viewModel.onIntent(EnterAmountIntent.ToAccountChanged(it))
                        },
                        onClosePressed = {
                            popBackStack(SwapGraph.EnterAmount, false)
                        },
                        onBackPressed = ::navigateUp
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
