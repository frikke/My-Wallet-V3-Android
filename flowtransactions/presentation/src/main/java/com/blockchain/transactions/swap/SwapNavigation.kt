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
import com.blockchain.koin.payloadScope
import com.blockchain.transactions.swap.confirmation.composable.ConfirmationArgs
import com.blockchain.transactions.swap.confirmation.composable.ConfirmationScreen
import com.blockchain.transactions.swap.enteramount.EnterAmountIntent
import com.blockchain.transactions.swap.enteramount.EnterAmountViewModel
import com.blockchain.transactions.swap.enteramount.composable.EnterAmount
import com.blockchain.transactions.swap.selectsource.composable.SelectSourceScreen
import com.blockchain.transactions.swap.selecttarget.composable.SelectTargetAsset
import com.blockchain.transactions.swap.selecttargetaccount.composable.SelectTargetAccount
import com.blockchain.transactions.swap.selecttargetaccount.composable.SelectTargetAccountArgs
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import org.koin.androidx.compose.getViewModel

object SwapGraph : BetterNavGraph() {
    object EnterAmount : BetterDestination()
    object SourceAccounts : BetterDestination()
    object SelectTarget : BetterDestinationWithArgs<String>()
    object TargetAsset : BetterDestinationWithArgs<String>()
    object TargetAccount : BetterDestinationWithArgs<SelectTargetAccountArgs>()
    object Confirmation : BetterDestinationWithArgs<ConfirmationArgs>()
}

@ExperimentalMaterialNavigationApi
fun NavGraphBuilder.swapGraphHost() {
    // TODO(aromano): navigation TEMP
    composable(SwapGraph::class.java.name) {
        val viewModel: EnterAmountViewModel = getViewModel(scope = payloadScope)

        BetterNavHost(
            startDestination = SwapGraph.EnterAmount,
        ) {
            betterDestination(SwapGraph.EnterAmount) {
                ChromeSingleScreen {
                    EnterAmount(
                        viewModel = viewModel,
                        navContextProvider = { this },
                        onBackPressed = ::navigateUp
                    )
                }
            }

            betterSheetDestination(SwapGraph.SourceAccounts) {
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
            betterSheetDestination(SwapGraph.TargetAsset) { sourceTicker ->
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

            betterSheetDestination(SwapGraph.TargetAccount) { args ->
                ChromeBottomSheet(onClose = ::navigateUp) {
                    SelectTargetAccount(
                        sourceTicker = args.sourceTicker,
                        targetTicker = args.targetTicker,
                        mode = args.mode,
                        accountSelected = {
                            viewModel.onIntent(EnterAmountIntent.ToAccountChanged(it))
                        },
                        onClosePressed = {
                            navController.popBackStack(SwapGraph.EnterAmount.route, false)
                        },
                        onBackPressed = ::navigateUp
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
