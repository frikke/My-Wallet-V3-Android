package com.blockchain.transactions.sell

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
import com.blockchain.koin.payloadScope
import com.blockchain.transactions.common.entersecondpassword.EnterSecondPasswordArgs
import com.blockchain.transactions.common.entersecondpassword.composable.EnterSecondPasswordScreen
import com.blockchain.transactions.sell.confirmation.SellConfirmationArgs
import com.blockchain.transactions.sell.confirmation.composable.SellConfirmationScreen
import com.blockchain.transactions.sell.enteramount.SellEnterAmountArgs
import com.blockchain.transactions.sell.enteramount.SellEnterAmountInputError
import com.blockchain.transactions.sell.enteramount.SellEnterAmountIntent
import com.blockchain.transactions.sell.enteramount.SellEnterAmountViewModel
import com.blockchain.transactions.sell.enteramount.composable.SellEnterAmount
import com.blockchain.transactions.sell.enteramount.composable.SellInputErrorScreen
import com.blockchain.transactions.sell.neworderstate.composable.SellNewOrderStateArgs
import com.blockchain.transactions.sell.neworderstate.composable.SellNewOrderStateScreen
import com.blockchain.transactions.sell.sourceaccounts.composable.SellSourceAccounts
import com.blockchain.transactions.sell.targetassets.SellTargetAssetsArgs
import com.blockchain.transactions.sell.targetassets.composable.SellTargetAssets
import com.blockchain.transactions.sell.upsell.UpsellBuyAfterSellScreen
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import info.blockchain.balance.AssetInfo
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

object SellGraph : NavGraph() {
    object EnterAmount : Destination()
    object InputError : DestinationWithArgs<SellEnterAmountInputError>()
    object SourceAccounts : Destination()
    object EnterSecondPassword : DestinationWithArgs<EnterSecondPasswordArgs>()
    object TargetAsset : DestinationWithArgs<SellTargetAssetsArgs>()
    object Confirmation : DestinationWithArgs<SellConfirmationArgs>()
    object NewOrderState : DestinationWithArgs<SellNewOrderStateArgs>()
    object UpsellBuy : DestinationWithArgs<String>()
}

@ExperimentalMaterialNavigationApi
@Composable
fun SellGraphHost(
    initialSourceAccount: CryptoAccount?,
    navigateToBuy: (AssetInfo) -> Unit,
    exitFlow: () -> Unit,
) {
    val viewModel: SellEnterAmountViewModel = getViewModel(
        scope = payloadScope,
        parameters = {
            parametersOf(SellEnterAmountArgs(Bindable(initialSourceAccount)))
        }
    )

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
            ChromeBottomSheet(onClose = ::navigateUp) {
                SellInputErrorScreen(
                    inputError = args,
                    closeClicked = ::navigateUp,
                )
            }
        }

        typedBottomSheet(SellGraph.SourceAccounts) {
            ChromeBottomSheet(fillMaxHeight = true, onClose = ::navigateUp) {
                SellSourceAccounts(
                    accountSelected = {
                        viewModel.onIntent(SellEnterAmountIntent.FromAccountChanged(it, null))
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
            ChromeBottomSheet(fillMaxHeight = true, onClose = ::navigateUp) {
                EnterSecondPasswordScreen(
                    args = args,
                    onAccountSecondPasswordValidated = { account, secondPassword ->
                        viewModel.onIntent(SellEnterAmountIntent.FromAccountChanged(account, secondPassword))
                        popBackStack(SellGraph.EnterAmount, false)
                    },
                    onBackPressed = ::navigateUp
                )
            }
        }

        // support nested graph navigation(...)
        typedBottomSheet(SellGraph.TargetAsset) { args ->
            ChromeBottomSheet(fillMaxHeight = true, onClose = ::navigateUp) {
                SellTargetAssets(
                    args = args,
                    accountSelected = { fromAccount, secondPassword, toAccount ->
                        viewModel.onIntent(
                            SellEnterAmountIntent.FromAndToAccountsChanged(
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
                SellConfirmationScreen(
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
                SellNewOrderStateScreen(
                    args = args,
                    exitFlow = exitFlow,
                )
            }
        }

        typedBottomSheet(SellGraph.UpsellBuy) { assetJustSoldTicker ->
            ChromeBottomSheet(fillMaxHeight = true, onClose = exitFlow) {
                UpsellBuyAfterSellScreen(
                    assetJustSoldTicker = assetJustSoldTicker,
                    navigateToBuy = { asset ->
                        navigateToBuy(asset)
                        exitFlow()
                    },
                    exitFlow = exitFlow,
                )
            }
        }
    }
}
