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
import com.blockchain.blockchaincard.ui.composables.ordercard.OrderCard
import com.blockchain.blockchaincard.ui.composables.ordercard.ProductDetails
import com.blockchain.blockchaincard.ui.composables.ordercard.ProductLegalInfo
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
        else BlockchainCardDestination.OrderCardDestination

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

        composable(BlockchainCardDestination.OrderCardDestination) {
            OrderCard(viewModel as OrderCardViewModel)
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
            CardCreationFailed(
                onTryAgain = {
                    viewModel.onIntent(BlockchainCardIntent.RetryOrderCard)
                }
            )
        }

        bottomSheet(BlockchainCardDestination.SeeProductDetailsDestination) {
            ProductDetails(
                onCloseProductDetailsBottomSheet = {
                    viewModel.onIntent(BlockchainCardIntent.HideBottomSheet)
                },
                onSeeProductLegalInfo = {
                    viewModel.onIntent(BlockchainCardIntent.OnSeeProductLegalInfo)
                }
            )
        }

        bottomSheet(BlockchainCardDestination.SeeProductLegalInfoDestination) {
            ProductLegalInfo(
                onCloseProductLegalInfoBottomSheet = {
                    viewModel.onIntent(BlockchainCardIntent.HideBottomSheet)
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
                        },
                        onChoosePaymentMethod = {
                            viewModel.onIntent(BlockchainCardIntent.ChoosePaymentMethod)
                        },
                        onTopUp = {
                            viewModel.onIntent(BlockchainCardIntent.TopUp)
                        },
                        onRefreshBalance = {
                            viewModel.onIntent(BlockchainCardIntent.LoadLinkedAccount)
                        }
                    )
                }
            }
        }

        bottomSheet(BlockchainCardDestination.ManageCardDetailsDestination) {
            state?.card?.let { card ->
                ManageCardDetails(
                    onDeleteCard = { viewModel.onIntent(BlockchainCardIntent.DeleteCard) },
                    onToggleLockCard = { isChecked: Boolean ->
                        if (isChecked) viewModel.onIntent(BlockchainCardIntent.LockCard)
                        else viewModel.onIntent(BlockchainCardIntent.UnlockCard)
                    },
                    cardStatus = card.status
                )
            }
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
