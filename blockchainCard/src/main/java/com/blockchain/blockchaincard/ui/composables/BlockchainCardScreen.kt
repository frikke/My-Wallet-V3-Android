package com.blockchain.blockchaincard.ui.composables

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarResult
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.blockchain.blockchaincard.R
import com.blockchain.blockchaincard.domain.models.BlockchainCardError
import com.blockchain.blockchaincard.ui.composables.managecard.AccountPicker
import com.blockchain.blockchaincard.ui.composables.managecard.BillingAddress
import com.blockchain.blockchaincard.ui.composables.managecard.BillingAddressUpdated
import com.blockchain.blockchaincard.ui.composables.managecard.CardTransactionDetails
import com.blockchain.blockchaincard.ui.composables.managecard.ManageCard
import com.blockchain.blockchaincard.ui.composables.managecard.ManageCardDetails
import com.blockchain.blockchaincard.ui.composables.managecard.PersonalDetails
import com.blockchain.blockchaincard.ui.composables.managecard.Support
import com.blockchain.blockchaincard.ui.composables.managecard.SupportPage
import com.blockchain.blockchaincard.ui.composables.managecard.TerminateCard
import com.blockchain.blockchaincard.ui.composables.managecard.TransactionControls
import com.blockchain.blockchaincard.ui.composables.ordercard.CardCreationFailed
import com.blockchain.blockchaincard.ui.composables.ordercard.CardCreationInProgress
import com.blockchain.blockchaincard.ui.composables.ordercard.CardCreationSuccess
import com.blockchain.blockchaincard.ui.composables.ordercard.LegalDocument
import com.blockchain.blockchaincard.ui.composables.ordercard.LegalDocumentsViewer
import com.blockchain.blockchaincard.ui.composables.ordercard.OrderCard
import com.blockchain.blockchaincard.ui.composables.ordercard.OrderCardAddressKYC
import com.blockchain.blockchaincard.ui.composables.ordercard.OrderCardContent
import com.blockchain.blockchaincard.ui.composables.ordercard.OrderCardSsnKYC
import com.blockchain.blockchaincard.ui.composables.ordercard.ProductDetails
import com.blockchain.blockchaincard.ui.composables.ordercard.ProductLegalInfo
import com.blockchain.blockchaincard.viewmodel.BlockchainCardArgs
import com.blockchain.blockchaincard.viewmodel.BlockchainCardDestination
import com.blockchain.blockchaincard.viewmodel.BlockchainCardErrorState
import com.blockchain.blockchaincard.viewmodel.BlockchainCardIntent
import com.blockchain.blockchaincard.viewmodel.BlockchainCardNavigationRouter
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewModel
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewState
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

    BlockchainCardScaffold(
        state = state,
        onSnackbarDismissed = { viewModel.onIntent(BlockchainCardIntent.SnackbarDismissed) }
    ) {
        MviBottomSheetNavHost(
            navEvents = navEventsFlowLifecycleAware,
            navigationRouter = BlockchainCardNavigationRouter(rememberNavController()),
            startDestination = startDestination,
        ) {

            composable(BlockchainCardDestination.OrderCardDestination) {
                OrderCard(viewModel as OrderCardViewModel)
            }

            composable(BlockchainCardDestination.OrderCardKycAddressDestination) {
                state?.let { state ->
                    OrderCardAddressKYC(
                        onContinue = { viewModel.onIntent(BlockchainCardIntent.OrderCardSSNAddress) },
                        onCheckBillingAddress = { viewModel.onIntent(BlockchainCardIntent.SeeBillingAddress) },
                        shortAddress = state.residentialAddress?.getShortAddress()
                    )
                }
            }

            composable(BlockchainCardDestination.OrderCardKycSSNDestination) {
                OrderCardSsnKYC(
                    onContinue = { ssn -> viewModel.onIntent(BlockchainCardIntent.OrderCardKycComplete(ssn)) }
                )
            }

            composable(BlockchainCardDestination.OrderCardConfirmDestination) {
                state?.let { state ->
                    OrderCardContent(
                        isLegalDocReviewComplete = state.isLegalDocReviewComplete,
                        onCreateCard = {
                            viewModel.onIntent(BlockchainCardIntent.CreateCard)
                        },
                        onSeeProductDetails = {
                            viewModel.onIntent(BlockchainCardIntent.OnSeeProductDetails)
                        },
                        onSeeLegalDocuments = {
                            viewModel.onIntent(BlockchainCardIntent.OnSeeLegalDocuments)
                        }
                    )
                }
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
                (state?.errorState as? BlockchainCardErrorState.ScreenErrorState)?.let { screenErrorState ->
                    val errorTitle = when (screenErrorState.error) {
                        is BlockchainCardError.LocalCopyBlockchainCardError -> {
                            stringResource(id = R.string.card_creation_failed)
                        }
                        is BlockchainCardError.UXBlockchainCardError -> {
                            screenErrorState.error.uxError.title
                        }
                    }
                    val errorDescription = when (screenErrorState.error) {
                        is BlockchainCardError.LocalCopyBlockchainCardError -> {
                            stringResource(id = R.string.card_creation_failed_description)
                        }
                        is BlockchainCardError.UXBlockchainCardError -> {
                            screenErrorState.error.uxError.description
                        }
                    }

                    CardCreationFailed(
                        errorTitle = errorTitle,
                        errorDescription = errorDescription,
                        onTryAgain = { viewModel.onIntent(BlockchainCardIntent.RetryOrderCard) }
                    )
                }
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
                state?.legalDocuments?.let { legalDocuments ->
                    ProductLegalInfo(
                        legalDocuments = legalDocuments,
                        onCloseProductLegalInfoBottomSheet = {
                            viewModel.onIntent(BlockchainCardIntent.HideBottomSheet)
                        },
                        onSeeLegalDocument = { legalDocument ->
                            viewModel.onIntent(BlockchainCardIntent.OnSeeSingleLegalDocument(legalDocument))
                        }
                    )
                }
            }

            composable(BlockchainCardDestination.LegalDocumentsDestination) {
                state?.legalDocuments?.let { legalDocuments ->
                    LegalDocumentsViewer(
                        legalDocuments = legalDocuments,
                        onLegalDocSeen = { documentName ->
                            viewModel.onIntent(BlockchainCardIntent.OnLegalDocSeen(documentName))
                        },
                        onFinish = {
                            viewModel.onIntent(BlockchainCardIntent.OnFinishLegalDocReview)
                        }
                    )
                }
            }

            composable(BlockchainCardDestination.SingleLegalDocumentDestination) {
                state?.singleLegalDocumentToSee?.let { legalDocument ->
                    LegalDocument(legalDocument = legalDocument)
                }
            }

            // Manage Card Screens
            composable(BlockchainCardDestination.ManageCardDestination) {
                if (viewModel is ManageCardViewModel) { // Once in the manage flow, the VM must be a ManageCardViewModel
                    state?.let { state ->
                        ManageCard(
                            card = state.card,
                            cardWidgetUrl = state.cardWidgetUrl,
                            linkedAccountBalance = state.linkedAccountBalance,
                            isBalanceLoading = state.isLinkedAccountBalanceLoading,
                            isTransactionListRefreshing = state.isTransactionListRefreshing,
                            transactionList = state.transactionList,
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
                            },
                            onSeeTransactionDetails = { transaction ->
                                viewModel.onIntent(BlockchainCardIntent.SeeTransactionDetails(transaction))
                            },
                            onRefreshTransactions = {
                                viewModel.onIntent(BlockchainCardIntent.RefreshTransactions)
                            },
                        )
                    }
                }
            }

            bottomSheet(BlockchainCardDestination.ManageCardDetailsDestination) {
                state?.card?.let { card ->
                    ManageCardDetails(
                        last4digits = card.last4,
                        onToggleLockCard = { isChecked: Boolean ->
                            if (isChecked) viewModel.onIntent(BlockchainCardIntent.LockCard)
                            else viewModel.onIntent(BlockchainCardIntent.UnlockCard)
                        },
                        cardStatus = card.status,
                        onSeePersonalDetails = { viewModel.onIntent(BlockchainCardIntent.SeePersonalDetails) },
                        onSeeTransactionControls = { viewModel.onIntent(BlockchainCardIntent.SeeTransactionControls) },
                        onSeeSupport = { viewModel.onIntent(BlockchainCardIntent.SeeSupport) },
                        onCloseBottomSheet = { viewModel.onIntent(BlockchainCardIntent.HideBottomSheet) }
                    )
                }
            }

            composable(BlockchainCardDestination.ChoosePaymentMethodDestination) {
                state?.let {
                    AccountPicker(
                        eligibleTradingAccountBalances = it.eligibleTradingAccountBalances,
                        onAccountSelected = { accountCurrencyNetworkTicker ->
                            viewModel.onIntent(
                                BlockchainCardIntent.LinkSelectedAccount(
                                    accountCurrencyNetworkTicker = accountCurrencyNetworkTicker
                                )
                            )
                        },
                        onCloseBottomSheet = {
                            viewModel.onIntent(BlockchainCardIntent.HideBottomSheet)
                        }
                    )
                }
            }

            bottomSheet(BlockchainCardDestination.PersonalDetailsDestination) {
                state?.let { state ->
                    PersonalDetails(
                        shortAddress = state.residentialAddress?.getShortAddress(),
                        onCheckBillingAddress = { viewModel.onIntent(BlockchainCardIntent.SeeBillingAddress) },
                        onCloseBottomSheet = { viewModel.onIntent(BlockchainCardIntent.HideBottomSheet) }
                    )
                }
            }

            bottomSheet(BlockchainCardDestination.BillingAddressDestination) {
                state?.let { state ->
                    state.residentialAddress?.let { residentialAddress ->
                        BillingAddress(
                            address = residentialAddress,
                            stateList = state.countryStateList,
                            onUpdateAddress = { newAddress ->
                                viewModel.onIntent(BlockchainCardIntent.UpdateBillingAddress(newAddress = newAddress))
                            },
                            onCloseBottomSheet = {
                                viewModel.onIntent(BlockchainCardIntent.HideBottomSheet)
                            }
                        )
                    }
                }
            }

            bottomSheet(BlockchainCardDestination.BillingAddressUpdateSuccessDestination) {
                BillingAddressUpdated(
                    success = true,
                    onDismiss = {
                        viewModel.onIntent(BlockchainCardIntent.DismissBillingAddressUpdateResult)
                    },
                    onCloseBottomSheet = {
                        viewModel.onIntent(BlockchainCardIntent.HideBottomSheet)
                    }
                )
            }

            bottomSheet(BlockchainCardDestination.BillingAddressUpdateFailedDestination) {
                state?.errorState?.let { errorState ->
                    BillingAddressUpdated(
                        success = false,
                        error = errorState.error,
                        onDismiss = {
                            viewModel.onIntent(BlockchainCardIntent.DismissBillingAddressUpdateResult)
                        },
                        onCloseBottomSheet = {
                            viewModel.onIntent(BlockchainCardIntent.HideBottomSheet)
                        }
                    )
                }
            }

            bottomSheet(BlockchainCardDestination.TransactionControlsDestination) {
                TransactionControls(onCloseBottomSheet = {
                    viewModel.onIntent(BlockchainCardIntent.HideBottomSheet)
                })
            }

            bottomSheet(BlockchainCardDestination.SupportDestination) {
                Support(
                    onCloseCard = {
                        viewModel.onIntent(BlockchainCardIntent.CloseCard)
                    },
                    onCloseBottomSheet = {
                        viewModel.onIntent(BlockchainCardIntent.HideBottomSheet)
                    },
                    onClickCardLost = {
                        viewModel.onIntent(BlockchainCardIntent.SeeCardLostPage)
                    },
                    onClickFAQ = {
                        viewModel.onIntent(BlockchainCardIntent.SeeFAQPage)
                    },
                    onClickContactSupport = {
                        viewModel.onIntent(BlockchainCardIntent.SeeContactSupportPage)
                    }
                )
            }

            bottomSheet(BlockchainCardDestination.CloseCardDestination) {
                state?.card?.last4?.let { last4 ->
                    TerminateCard(
                        last4digits = last4,
                        onConfirmCloseCard = {
                            viewModel.onIntent(BlockchainCardIntent.ConfirmCloseCard)
                        },
                        onCloseBottomSheet = {
                            viewModel.onIntent(BlockchainCardIntent.HideBottomSheet)
                        }
                    )
                }
            }

            bottomSheet(BlockchainCardDestination.TransactionDetailsDestination) {
                state?.selectedCardTransaction?.let { transaction ->
                    CardTransactionDetails(
                        cardTransaction = transaction,
                        onCloseBottomSheet = {
                            viewModel.onIntent(BlockchainCardIntent.HideBottomSheet)
                        }
                    )
                }
            }

            bottomSheet(BlockchainCardDestination.TransactionDetailsDestination) {
                state?.selectedCardTransaction?.let { transaction ->
                    CardTransactionDetails(
                        cardTransaction = transaction,
                        onCloseBottomSheet = { viewModel.onIntent(BlockchainCardIntent.HideBottomSheet) }
                    )
                }
            }

            composable(BlockchainCardDestination.CardLostPageDestination) {
                SupportPage()
            }

            composable(BlockchainCardDestination.FAQPageDestination) {
                SupportPage()
            }

            composable(BlockchainCardDestination.ContactSupportPageDestination) {
                SupportPage()
            }
        }
    }
}

@Composable
fun BlockchainCardScaffold(
    state: BlockchainCardViewState?,
    onSnackbarDismissed: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {

    val scaffoldState = rememberScaffoldState()
    val context = LocalContext.current
    LaunchedEffect(state?.errorState) {
        (state?.errorState as? BlockchainCardErrorState.SnackbarErrorState)?.let { snackbarErrorState ->
            val errorTitle = when (snackbarErrorState.error) {
                is BlockchainCardError.LocalCopyBlockchainCardError -> context.getString(R.string.common_error)
                is BlockchainCardError.UXBlockchainCardError -> snackbarErrorState.error.uxError.title
            }
            val errorDescription = when (snackbarErrorState.error) {
                is BlockchainCardError.LocalCopyBlockchainCardError -> ""
                is BlockchainCardError.UXBlockchainCardError -> snackbarErrorState.error.uxError.description
            }

            val snackbarResult = scaffoldState.snackbarHostState.showSnackbar("$errorTitle. $errorDescription")
            if (snackbarResult == SnackbarResult.Dismissed) onSnackbarDismissed()
        }
    }

    Scaffold(scaffoldState = scaffoldState, content = content)
}
