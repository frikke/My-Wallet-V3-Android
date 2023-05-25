package com.blockchain.transactions.sell

import androidx.activity.compose.BackHandler
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
import com.blockchain.transactions.sell.confirmation.SellConfirmationArgs
import com.blockchain.transactions.sell.confirmation.composable.ConfirmationScreen
import com.blockchain.transactions.sell.enteramount.EnterAmountIntent
import com.blockchain.transactions.sell.enteramount.EnterAmountViewModel
import com.blockchain.transactions.sell.enteramount.SellEnterAmountInputError
import com.blockchain.transactions.sell.enteramount.composable.InputErrorScreen
import com.blockchain.transactions.sell.enteramount.composable.SellEnterAmount
import com.blockchain.transactions.sell.neworderstate.composable.NewOrderStateArgs
import com.blockchain.transactions.sell.neworderstate.composable.NewOrderStateScreen
import com.blockchain.transactions.sell.sourceaccounts.composable.SourceAccounts
import com.blockchain.transactions.sell.targetassets.TargetAssetsArgs
import com.blockchain.transactions.sell.targetassets.composable.TargetAssets
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

object SellGraph : NavGraph() {
    object EnterAmount : Destination()
    object InputError : DestinationWithArgs<SellEnterAmountInputError>()
    object SourceAccounts : Destination()
    object EnterSecondPassword : DestinationWithArgs<EnterSecondPasswordArgs>()
    object TargetAsset : DestinationWithArgs<TargetAssetsArgs>()
    object Confirmation : DestinationWithArgs<SellConfirmationArgs>()
    object NewOrderState : DestinationWithArgs<NewOrderStateArgs>()
}

@ExperimentalMaterialNavigationApi
@Composable
fun SellGraphHost(
    exitFlow: () -> Unit,
) {
    val viewModel: EnterAmountViewModel = getViewModel(scope = payloadScope)

    TypedNavHost(
        graph = SellGraph,
        startDestination = SellGraph.EnterAmount,
    ) {
        typedComposable(SellGraph.EnterAmount) {
            ChromeSingleScreen {
                SellEnterAmount(
                    viewModel = viewModel,
                    onBackPressed = exitFlow
                )
            }
        }

        typedBottomSheet(SellGraph.InputError) { args ->
            BackHandler(onBack = ::navigateUp)
            InputErrorScreen(
                inputError = args,
                closeClicked = ::navigateUp,
            )
        }

        typedBottomSheet(SellGraph.SourceAccounts) {
            ChromeBottomSheet(onClose = ::navigateUp) {
                SourceAccounts(
                    accountSelected = {
                        viewModel.onIntent(EnterAmountIntent.FromAccountChanged(it, null))
                    },
                    navigateToEnterSecondPassword = { account ->
                        navigateTo(
                            SellGraph.EnterSecondPassword,
                            EnterSecondPasswordArgs(Bindable(account))
                        )
                    },
                    onBackPressed = ::navigateUp
                )
            }
        }

        typedBottomSheet(SellGraph.EnterSecondPassword) { args ->
            ChromeBottomSheet(onClose = ::navigateUp) {
                EnterSecondPasswordScreen(
                    args = args,
                    onAccountSecondPasswordValidated = { account, secondPassword ->
                        viewModel.onIntent(EnterAmountIntent.FromAccountChanged(account, secondPassword))
                        popBackStack(SellGraph.EnterAmount, false)
                    },
                    onBackPressed = ::navigateUp
                )
            }
        }

        // support nested graph navigation(...)
        typedBottomSheet(SellGraph.TargetAsset) { args ->
            ChromeBottomSheet(onClose = ::navigateUp) {
                TargetAssets(
                    args = args,
                    accountSelected = { fromAccount, secondPassword, toAccount ->
                        viewModel.onIntent(
                            EnterAmountIntent.FromAndToAccountsChanged(
                                fromAccount = fromAccount,
                                secondPassword = secondPassword,
                                toAccount = toAccount,
                            )
                        )
                    },
                    onClosePressed = ::navigateUp
                )
            }
        }

        typedComposable(SellGraph.Confirmation) { args ->
            ChromeSingleScreen {
                ConfirmationScreen(
                    args = args,
                    openNewOrderState = { args ->
                        navigateTo(SellGraph.NewOrderState, args) {
                            popUpTo(SellGraph)
                        }
                    },
                    backClicked = { navigateUp() },
                )
            }
        }

        typedComposable(SellGraph.NewOrderState) { args ->
            ChromeSingleScreen {
                NewOrderStateScreen(
                    args = args,
                    exitFlow = exitFlow,
                )
            }
        }
    }
}
