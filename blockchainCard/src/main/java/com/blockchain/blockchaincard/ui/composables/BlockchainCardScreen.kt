package com.blockchain.blockchaincard.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.blockchain.blockchaincard.ui.composables.managecard.AccountPicker
import com.blockchain.blockchaincard.ui.composables.managecard.ManageCard
import com.blockchain.blockchaincard.ui.composables.managecard.ManageCardDetails
import com.blockchain.blockchaincard.ui.composables.ordercard.CardCreationFailed
import com.blockchain.blockchaincard.ui.composables.ordercard.CardCreationInProgress
import com.blockchain.blockchaincard.ui.composables.ordercard.CardCreationSuccess
import com.blockchain.blockchaincard.ui.composables.ordercard.OrderOrLinkCard
import com.blockchain.blockchaincard.ui.composables.ordercard.ProductDetails
import com.blockchain.blockchaincard.ui.composables.ordercard.SelectCardForOrder
import com.blockchain.blockchaincard.viewmodel.BlockchainCardArgs
import com.blockchain.blockchaincard.viewmodel.BlockchainCardDestination
import com.blockchain.blockchaincard.viewmodel.BlockchainCardIntent
import com.blockchain.blockchaincard.viewmodel.BlockchainCardNavigationRouter
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewModel
import com.blockchain.blockchaincard.viewmodel.managecard.ManageCardViewModel
import com.blockchain.blockchaincard.viewmodel.ordercard.OrderCardViewModel
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.compose.MviBottomSheetNavHost
import com.blockchain.commonarch.presentation.mvi_v2.compose.bottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi

@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
fun BlockchainCardNavHost(
    viewModel: BlockchainCardViewModel,
    modelArgs: ModelConfigArgs
) {
    viewModel.viewCreated(modelArgs)

    val startDestination =
        if (modelArgs is BlockchainCardArgs.CardArgs) BlockchainCardDestination.ManageCardDestination
        else BlockchainCardDestination.OrderOrLinkCardDestination

    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val navEventsFlowLifecycleAware = remember(viewModel.navigationEventFlow, lifecycleOwner) {
        viewModel.navigationEventFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val state by stateFlowLifecycleAware.collectAsState(null)

    MviBottomSheetNavHost(
        navEvents = navEventsFlowLifecycleAware,
        navigationRouter = BlockchainCardNavigationRouter(rememberNavController()),
        startDestination = startDestination,
    ) {

        composable(BlockchainCardDestination.OrderOrLinkCardDestination) {
            OrderOrLinkCard(viewModel as OrderCardViewModel)
        }

        composable(BlockchainCardDestination.SelectCardForOrderDestination) {
            SelectCardForOrder(
                onCreateCard = {
                    viewModel.onIntent(
                        // TODO(labreu): once staging API is not harcoded, remove this
                        BlockchainCardIntent.CreateCard(
                            productCode = "VIRTUAL1",
                            ssn = "111111110"
                        )
                    )
                },
                onSeeProductDetails = {
                    viewModel.onIntent(
                        BlockchainCardIntent.OnSeeProductDetails
                    )
                }
            )
        }

        composable(BlockchainCardDestination.CreateCardInProgressDestination) {
            CardCreationInProgress()
        }

        composable(BlockchainCardDestination.CreateCardSuccessDestination) {
            CardCreationSuccess(
                onFinish = {
                    viewModel.onIntent(BlockchainCardIntent.ManageCard)
                }
            )
        }

        composable(BlockchainCardDestination.CreateCardFailedDestination) {
            CardCreationFailed()
        }

        bottomSheet(BlockchainCardDestination.SeeProductDetailsDestination) {
            ProductDetails(
                cardProduct = state?.cardProduct,
                onCloseProductDetailsBottomSheet = {
                    viewModel.onIntent(BlockchainCardIntent.HideProductDetailsBottomSheet)
                }
            )
        }

        // Manage Card Screens
        composable(BlockchainCardDestination.ManageCardDestination) {
            if (viewModel is ManageCardViewModel) { // Once in the manage flow, the VM must be a ManageCardViewModel
                state?.let { state ->
                    ManageCard(
                        card = state.card,
                        cardWidgetUrl = state.cardWidgetUrl,
                        isBalanceLoading = state.isLinkedAccountBalanceLoading,
                        linkedAccountBalance = state.linkedAccountBalance,
                        onManageCardDetails = {
                            viewModel.onIntent(BlockchainCardIntent.ManageCardDetails)
                        }
                    ) {
                        viewModel.onIntent(BlockchainCardIntent.ChoosePaymentMethod)
                    }
                }
            }
        }

        bottomSheet(BlockchainCardDestination.ManageCardDetailsDestination) {
            ManageCardDetails(onDeleteCard = { viewModel.onIntent(BlockchainCardIntent.DeleteCard) })
        }

        bottomSheet(BlockchainCardDestination.ChoosePaymentMethodDestination) {
            state?.let {
                AccountPicker(it.eligibleTradingAccountBalances) { accountCurrencyNetworkTicker ->
                    viewModel.onIntent(
                        BlockchainCardIntent.LinkSelectedAccount(
                            accountCurrencyNetworkTicker = accountCurrencyNetworkTicker
                        )
                    )
                }
            }
        }
    }
}
