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
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.impl.CustodialInterestAccount
import com.blockchain.koin.payloadScope
import com.blockchain.transactions.common.entersecondpassword.EnterSecondPasswordArgs
import com.blockchain.transactions.common.entersecondpassword.composable.EnterSecondPassword
import com.blockchain.transactions.swap.confirmation.SwapConfirmationArgs
import com.blockchain.transactions.swap.confirmation.composable.SwapConfirmationScreen
import com.blockchain.transactions.swap.enteramount.SwapEnterAmountArgs
import com.blockchain.transactions.swap.enteramount.SwapEnterAmountInputError
import com.blockchain.transactions.swap.enteramount.SwapEnterAmountIntent
import com.blockchain.transactions.swap.enteramount.SwapEnterAmountViewModel
import com.blockchain.transactions.swap.enteramount.composable.SwapEnterAmount
import com.blockchain.transactions.swap.enteramount.composable.SwapInputErrorScreen
import com.blockchain.transactions.swap.neworderstate.composable.SwapNewOrderStateArgs
import com.blockchain.transactions.swap.neworderstate.composable.SwapNewOrderStateScreen
import com.blockchain.transactions.swap.sourceaccounts.composable.SwapSourceAccounts
import com.blockchain.transactions.swap.targetaccounts.composable.SwapTargetAccounts
import com.blockchain.transactions.swap.targetaccounts.composable.SwapTargetAccountsArgs
import com.blockchain.transactions.swap.targetassets.composable.SwapTargetAssets
import com.blockchain.transactions.upsell.interest.UpsellInterestAfterSwapScreen
import com.blockchain.transactions.upsell.interest.UpsellInterestArgs
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

object SwapGraph : NavGraph() {
    object EnterAmount : Destination()
    object InputError : DestinationWithArgs<SwapEnterAmountInputError>()
    object SourceAccounts : Destination()
    object EnterSecondPassword : DestinationWithArgs<EnterSecondPasswordArgs>()
    object TargetAsset : DestinationWithArgs<String>()
    object TargetAccount : DestinationWithArgs<SwapTargetAccountsArgs>()
    object Confirmation : DestinationWithArgs<SwapConfirmationArgs>()
    object NewOrderState : DestinationWithArgs<SwapNewOrderStateArgs>()
    object UpsellInterest : DestinationWithArgs<UpsellInterestArgs>()
}

@ExperimentalMaterialNavigationApi
@Composable
fun SwapGraphHost(
    initialSourceAccount: CryptoAccount?,
    navigateToInterestDeposit: (source: CryptoAccount, target: CustodialInterestAccount) -> Unit,
    exitFlow: () -> Unit,
) {
    val viewModel: SwapEnterAmountViewModel = getViewModel(
        scope = payloadScope,
        parameters = {
            parametersOf(SwapEnterAmountArgs(Bindable(initialSourceAccount)))
        }
    )

    TypedNavHost(
        graph = SwapGraph,
        startDestination = SwapGraph.EnterAmount
    ) {
        typedComposable(SwapGraph.EnterAmount) {
            ChromeSingleScreen {
                SwapEnterAmount(
                    viewModel = viewModel,
                    navContextProvider = { this },
                    onBackPressed = exitFlow
                )
            }
        }

        typedBottomSheet(SwapGraph.InputError) { args ->
            ChromeBottomSheet(onClose = ::navigateUp) {
                SwapInputErrorScreen(
                    inputError = args,
                    closeClicked = ::navigateUp,
                )
            }
        }

        typedBottomSheet(SwapGraph.SourceAccounts) {
            ChromeBottomSheet(fillMaxHeight = true, onClose = ::navigateUp) {
                SwapSourceAccounts(
                    accountSelected = {
                        viewModel.onIntent(SwapEnterAmountIntent.FromAccountChanged(it, null))
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
            ChromeBottomSheet(fillMaxHeight = true, onClose = ::navigateUp) {
                EnterSecondPassword(
                    args = args,
                    onAccountSecondPasswordValidated = { account, secondPassword ->
                        viewModel.onIntent(SwapEnterAmountIntent.FromAccountChanged(account, secondPassword))
                        popBackStack(SwapGraph.EnterAmount, false)
                    },
                    onBackPressed = ::navigateUp
                )
            }
        }

        // support nested graph navigation(...)
        typedBottomSheet(SwapGraph.TargetAsset) { sourceTicker ->
            ChromeBottomSheet(fillMaxHeight = true, onClose = ::navigateUp) {
                SwapTargetAssets(
                    sourceTicker = sourceTicker,
                    accountSelected = {
                        viewModel.onIntent(SwapEnterAmountIntent.ToAccountChanged(it))
                    },
                    navContextProvider = { this },
                    onClosePressed = ::navigateUp
                )
            }
        }

        typedBottomSheet(SwapGraph.TargetAccount) { args ->
            ChromeBottomSheet(fillMaxHeight = true, onClose = ::navigateUp) {
                SwapTargetAccounts(
                    sourceTicker = args.sourceTicker,
                    targetTicker = args.targetTicker,
                    mode = args.mode,
                    accountSelected = {
                        viewModel.onIntent(SwapEnterAmountIntent.ToAccountChanged(it))
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
                SwapConfirmationScreen(
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
                SwapNewOrderStateScreen(
                    args = args,
                    exitFlow = exitFlow
                )
            }
        }

        typedBottomSheet(SwapGraph.UpsellInterest) { args ->
            ChromeBottomSheet(fillMaxHeight = true, onClose = exitFlow) {
                UpsellInterestAfterSwapScreen(
                    args = args,
                    navigateToInterestDeposit = { source, target ->
                        navigateToInterestDeposit(source, target)
                        exitFlow()
                    },
                    exitFlow = exitFlow,
                )
            }
        }
    }
}
