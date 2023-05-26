package com.blockchain.transactions.swap

import androidx.compose.runtime.Composable
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
import com.blockchain.betternavigation.utils.Bindable
import com.blockchain.chrome.composable.ChromeBottomSheet
import com.blockchain.chrome.composable.ChromeSingleScreen
import com.blockchain.koin.payloadScope
import com.blockchain.transactions.common.entersecondpassword.EnterSecondPasswordArgs
import com.blockchain.transactions.common.entersecondpassword.composable.EnterSecondPasswordScreen
import com.blockchain.transactions.swap.confirmation.SwapConfirmationArgs
import com.blockchain.transactions.swap.confirmation.composable.ConfirmationScreen
import com.blockchain.transactions.swap.enteramount.EnterAmountIntent
import com.blockchain.transactions.swap.enteramount.EnterAmountViewModel
import com.blockchain.transactions.swap.enteramount.SwapEnterAmountInputError
import com.blockchain.transactions.swap.enteramount.composable.EnterAmount
import com.blockchain.transactions.swap.enteramount.composable.InputErrorScreen
import com.blockchain.transactions.swap.neworderstate.composable.NewOrderStateArgs
import com.blockchain.transactions.swap.neworderstate.composable.NewOrderStateScreen
import com.blockchain.transactions.swap.sourceaccounts.composable.SourceAccounts
import com.blockchain.transactions.swap.targetaccounts.composable.TargetAccounts
import com.blockchain.transactions.swap.targetaccounts.composable.TargetAccountsArgs
import com.blockchain.transactions.swap.targetassets.composable.TargetAssets
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import org.koin.androidx.compose.getViewModel

object SwapGraph : NavGraph() {
    object EnterAmount : Destination()
    object InputError : DestinationWithArgs<SwapEnterAmountInputError>()
    object SourceAccounts : Destination()
    object EnterSecondPassword : DestinationWithArgs<EnterSecondPasswordArgs>()
    object TargetAsset : DestinationWithArgs<String>()
    object TargetAccount : DestinationWithArgs<TargetAccountsArgs>()
    object Confirmation : DestinationWithArgs<SwapConfirmationArgs>()
    object NewOrderState : DestinationWithArgs<NewOrderStateArgs>()
}

@ExperimentalMaterialNavigationApi
@Composable
fun SwapGraphHost(
    exitFlow: () -> Unit,
) {
    val viewModel: EnterAmountViewModel = getViewModel(scope = payloadScope)

    TypedNavHost(
        graph = SwapGraph,
        startDestination = SwapGraph.EnterAmount
    ) {
        typedComposable(SwapGraph.EnterAmount) {
            ChromeSingleScreen {
                EnterAmount(
                    viewModel = viewModel,
                    navContextProvider = { this },
                    onBackPressed = exitFlow
                )
            }
        }

        typedBottomSheet(SwapGraph.InputError) { args ->
            ChromeBottomSheet(fillMaxHeight = false, onClose = ::navigateUp) {
                InputErrorScreen(
                    inputError = args,
                    closeClicked = ::navigateUp,
                )
            }
        }

        typedBottomSheet(SwapGraph.SourceAccounts) {
            ChromeBottomSheet(onClose = ::navigateUp) {
                SourceAccounts(
                    accountSelected = {
                        viewModel.onIntent(EnterAmountIntent.FromAccountChanged(it, null))
                    },
                    navigateToEnterSecondPassword = { account ->
                        navigateTo(
                            SwapGraph.EnterSecondPassword,
                            EnterSecondPasswordArgs(Bindable(account))
                        )
                    },
                    onBackPressed = ::navigateUp
                )
            }
        }

        typedBottomSheet(SwapGraph.EnterSecondPassword) { args ->
            ChromeBottomSheet(onClose = ::navigateUp) {
                EnterSecondPasswordScreen(
                    args = args,
                    onAccountSecondPasswordValidated = { account, secondPassword ->
                        viewModel.onIntent(EnterAmountIntent.FromAccountChanged(account, secondPassword))
                        popBackStack(SwapGraph.EnterAmount, false)
                    },
                    onBackPressed = ::navigateUp
                )
            }
        }

        // support nested graph navigation(...)
        typedBottomSheet(SwapGraph.TargetAsset) { sourceTicker ->
            ChromeBottomSheet(onClose = ::navigateUp) {
                TargetAssets(
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
                TargetAccounts(
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
                    backClicked = { navigateUp() }
                )
            }
        }

        typedComposable(SwapGraph.NewOrderState) { args ->
            ChromeSingleScreen {
                NewOrderStateScreen(
                    args = args,
                    exitFlow = exitFlow
                )
            }
        }
    }
}
