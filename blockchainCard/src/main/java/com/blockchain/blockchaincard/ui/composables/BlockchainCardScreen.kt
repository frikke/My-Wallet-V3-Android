package com.blockchain.blockchaincard.ui.composables

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarResult
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.blockchain.blockchaincard.R
import com.blockchain.blockchaincard.domain.models.BlockchainCardError
import com.blockchain.blockchaincard.domain.models.BlockchainCardStatus
import com.blockchain.blockchaincard.ui.composables.managecard.AccountPicker
import com.blockchain.blockchaincard.ui.composables.managecard.BillingAddress
import com.blockchain.blockchaincard.ui.composables.managecard.BillingAddressUpdated
import com.blockchain.blockchaincard.ui.composables.managecard.CardActivationPage
import com.blockchain.blockchaincard.ui.composables.managecard.CardActivationSuccess
import com.blockchain.blockchaincard.ui.composables.managecard.CardSelector
import com.blockchain.blockchaincard.ui.composables.managecard.CardTransactionDetails
import com.blockchain.blockchaincard.ui.composables.managecard.CardTransactionHistory
import com.blockchain.blockchaincard.ui.composables.managecard.Documents
import com.blockchain.blockchaincard.ui.composables.managecard.FundingAccountActionChooser
import com.blockchain.blockchaincard.ui.composables.managecard.ManageCard
import com.blockchain.blockchaincard.ui.composables.managecard.ManageCardDetails
import com.blockchain.blockchaincard.ui.composables.managecard.PersonalDetails
import com.blockchain.blockchaincard.ui.composables.managecard.SetPinPage
import com.blockchain.blockchaincard.ui.composables.managecard.SetPinSuccess
import com.blockchain.blockchaincard.ui.composables.managecard.Support
import com.blockchain.blockchaincard.ui.composables.managecard.SupportPage
import com.blockchain.blockchaincard.ui.composables.managecard.TerminateCard
import com.blockchain.blockchaincard.ui.composables.managecard.TransactionControls
import com.blockchain.blockchaincard.ui.composables.ordercard.CardCreationFailed
import com.blockchain.blockchaincard.ui.composables.ordercard.CardCreationInProgress
import com.blockchain.blockchaincard.ui.composables.ordercard.CardCreationSuccess
import com.blockchain.blockchaincard.ui.composables.ordercard.CardProductPicker
import com.blockchain.blockchaincard.ui.composables.ordercard.HowToOrderCard
import com.blockchain.blockchaincard.ui.composables.ordercard.LegalDocument
import com.blockchain.blockchaincard.ui.composables.ordercard.LegalDocumentsViewer
import com.blockchain.blockchaincard.ui.composables.ordercard.LoadingKycStatus
import com.blockchain.blockchaincard.ui.composables.ordercard.OrderCardAddressKYC
import com.blockchain.blockchaincard.ui.composables.ordercard.OrderCardIntro
import com.blockchain.blockchaincard.ui.composables.ordercard.OrderCardKycFailure
import com.blockchain.blockchaincard.ui.composables.ordercard.OrderCardKycPending
import com.blockchain.blockchaincard.ui.composables.ordercard.OrderCardSsnKYC
import com.blockchain.blockchaincard.ui.composables.ordercard.ProductDetails
import com.blockchain.blockchaincard.ui.composables.ordercard.ProductLegalInfo
import com.blockchain.blockchaincard.ui.composables.ordercard.ReviewAndSubmit
import com.blockchain.blockchaincard.viewmodel.BlockchainCardArgs
import com.blockchain.blockchaincard.viewmodel.BlockchainCardDestination
import com.blockchain.blockchaincard.viewmodel.BlockchainCardErrorState
import com.blockchain.blockchaincard.viewmodel.BlockchainCardIntent
import com.blockchain.blockchaincard.viewmodel.BlockchainCardNavigationEvent
import com.blockchain.blockchaincard.viewmodel.BlockchainCardNavigationRouter
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewModel
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewState
import com.blockchain.blockchaincard.viewmodel.managecard.ManageCardViewModel
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.compose.MviBottomSheetNavHost
import com.blockchain.commonarch.presentation.mvi_v2.compose.bottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.componentlib.theme.AppTheme
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
fun BlockchainCardNavHost(
    viewModel: BlockchainCardViewModel,
    modelArgs: ModelConfigArgs,
    navigationRouter: BlockchainCardNavigationRouter,
    stateFlowLifecycleAware: Flow<BlockchainCardViewState>,
    navEventsFlowLifecycleAware: Flow<BlockchainCardNavigationEvent>
) {
    val startDestination by remember(modelArgs) {
        mutableStateOf(
            if (modelArgs is BlockchainCardArgs.CardArgs) {
                if (modelArgs.preselectedCard != null ||
                    modelArgs.cards.filter { it.status != BlockchainCardStatus.TERMINATED }.size == 1
                ) {
                    BlockchainCardDestination.ManageCardDestination
                } else {
                    BlockchainCardDestination.SelectCardDestination
                }
            } else
                BlockchainCardDestination.LoadingKycStatusDestination
        )
    }

    val state by stateFlowLifecycleAware.collectAsState(null)

    BlockchainCardScaffold(
        state = state,
        onSnackbarDismissed = { viewModel.onIntent(BlockchainCardIntent.SnackbarDismissed) }
    ) {
        MviBottomSheetNavHost(
            navEvents = navEventsFlowLifecycleAware,
            navigationRouter = navigationRouter,
            startDestination = startDestination,
        ) {
            composable(BlockchainCardDestination.LoadingKycStatusDestination) {
                LoadingKycStatus()
            }

            composable(BlockchainCardDestination.OrderCardIntroDestination) {
                OrderCardIntro(
                    onOrderCard = { viewModel.onIntent(BlockchainCardIntent.HowToOrderCard) }
                )
            }

            bottomSheet(BlockchainCardDestination.HowToOrderCardDestination) {
                HowToOrderCard(
                    onCloseBottomSheet = { viewModel.onIntent(BlockchainCardIntent.HideBottomSheet) },
                    onContinue = { viewModel.onIntent(BlockchainCardIntent.OrderCardPerformKyc) }
                )
            }

            composable(BlockchainCardDestination.OrderCardKycPendingDestination) {
                OrderCardKycPending(
                    onContinue = { viewModel.onIntent(BlockchainCardIntent.OrderCardKycComplete) }
                )
            }

            composable(BlockchainCardDestination.OrderCardKycFailureDestination) {
                state?.kycStatus?.errorFields?.let { errors ->
                    OrderCardKycFailure(
                        errorFields = errors,
                        onTryAgain = { viewModel.onIntent(BlockchainCardIntent.OrderCardPerformKyc) }
                    )
                }
            }

            composable(BlockchainCardDestination.OrderCardKycAddressDestination) {
                state?.let { state ->
                    OrderCardAddressKYC(
                        onContinue = { viewModel.onIntent(BlockchainCardIntent.OrderCardKycSSN) },
                        onCheckBillingAddress = { viewModel.onIntent(BlockchainCardIntent.SeeBillingAddress) },
                        line1 = state.residentialAddress?.line1,
                        city = state.residentialAddress?.city,
                        postalCode = state.residentialAddress?.postCode,
                        isAddressLoading = state.isAddressLoading
                    )
                }
            }

            composable(BlockchainCardDestination.OrderCardKycSSNDestination) {
                OrderCardSsnKYC(
                    onContinue = { ssn ->
                        viewModel.onIntent(BlockchainCardIntent.UpdateSSN(ssn))
                    }
                )
            }

            composable(BlockchainCardDestination.ChooseCardProductDestination) {
                state?.cardProductList?.let { blockchainCardProducts ->
                    CardProductPicker(
                        cardProducts = blockchainCardProducts,
                        onContinue = { cardProduct ->
                            viewModel.onIntent(BlockchainCardIntent.OnOrderCardConfirm(cardProduct))
                        },
                        onSeeProductDetails = {
                            viewModel.onIntent(BlockchainCardIntent.OnSeeProductDetails)
                        }
                    )
                }
            }

            composable(BlockchainCardDestination.ReviewAndSubmitCardDestination) {
                state?.let { state ->
                    ReviewAndSubmit(
                        firstAndLastName = state.userFirstAndLastName,
                        shippingAddress = state.shippingAddress,
                        cardProductType = state.selectedCardProduct?.type,
                        isLegalDocReviewComplete = state.isLegalDocReviewComplete,
                        onChangeShippingAddress = {
                            viewModel.onIntent(BlockchainCardIntent.OnChangeShippingAddress)
                        },
                        onSeeLegalDocuments = {
                            viewModel.onIntent(BlockchainCardIntent.OnSeeLegalDocuments)
                        },
                        onCreateCard = {
                            viewModel.onIntent(BlockchainCardIntent.CreateCard)
                        },
                        onChangeSelectedProduct = {
                            viewModel.onIntent(BlockchainCardIntent.OnChooseProduct)
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
                        state?.currentCard?.let {
                            viewModel.onIntent(BlockchainCardIntent.OnOrderCardFlowComplete)
                        }
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
                        legalDocuments = legalDocuments.filter { it.required },
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
            composable(BlockchainCardDestination.SelectCardDestination) {
                if (viewModel is ManageCardViewModel) {
                    state?.let { state ->
                        state.cardList?.let { cardList ->
                            CardSelector(
                                cards = cardList,
                                defaultCardId = state.defaultCardId,
                                hasProductsAvailableToOrder = state.cardProductList?.any { product ->
                                    product.remainingCards > 0
                                } ?: false,
                                onOrderCard = {
                                    viewModel.onIntent(BlockchainCardIntent.OrderCard)
                                },
                                onManageCard = { card ->
                                    viewModel.onIntent(BlockchainCardIntent.ManageCardDetails(card))
                                },
                                onViewCard = { card ->
                                    viewModel.onIntent(BlockchainCardIntent.ManageCard(card))
                                },
                                onSetCardAsDefault = { cardId ->
                                    viewModel.onIntent(BlockchainCardIntent.SaveCardAsDefault(cardId))
                                },
                                onRefreshCards = {
                                    viewModel.onIntent(BlockchainCardIntent.LoadCards)
                                },
                                onRefreshProducts = {
                                    viewModel.onIntent(BlockchainCardIntent.LoadProducts)
                                }
                            )
                        }
                    }
                }
            }

            composable(BlockchainCardDestination.ManageCardDestination) {
                if (viewModel is ManageCardViewModel) { // Once in the manage flow, the VM must be a ManageCardViewModel
                    state?.let { state ->
                        ManageCard(
                            card = state.currentCard,
                            cardWidgetUrl = state.cardWidgetUrl,
                            linkedAccountBalance = state.linkedAccountBalance,
                            isBalanceLoading = state.isLinkedAccountBalanceLoading,
                            isTransactionListRefreshing = state.isTransactionListRefreshing,
                            transactionList = state.shortTransactionList,
                            googleWalletState = state.googleWalletStatus,
                            cardOrderState = state.cardOrderState,
                            onViewCardSelector = { viewModel.onIntent(BlockchainCardIntent.SelectCard) },
                            onManageCardDetails = { card ->
                                viewModel.onIntent(BlockchainCardIntent.ManageCardDetails(card))
                            },
                            onFundingAccountClicked = {
                                viewModel.onIntent(BlockchainCardIntent.FundingAccountClicked)
                            },
                            onRefreshBalance = {
                                viewModel.onIntent(BlockchainCardIntent.LoadLinkedAccount)
                            },
                            onSeeAllTransactions = {
                                viewModel.onIntent(BlockchainCardIntent.SeeAllTransactions)
                            },
                            onSeeTransactionDetails = { transaction ->
                                viewModel.onIntent(BlockchainCardIntent.SeeTransactionDetails(transaction))
                            },
                            onRefreshTransactions = {
                                viewModel.onIntent(BlockchainCardIntent.RefreshTransactions)
                            },
                            onRefreshCardWidgetUrl = {
                                viewModel.onIntent(BlockchainCardIntent.LoadCardWidget)
                            },
                            onAddFunds = {
                                viewModel.onIntent(BlockchainCardIntent.AddFunds)
                            },
                            onAddToGoogleWallet = {
                                viewModel.onIntent(BlockchainCardIntent.LoadGoogleWalletPushTokenizeData)
                            },
                            onActivateCard = {
                                viewModel.onIntent(BlockchainCardIntent.ActivateCard)
                            },
                            onWebMessageReceived = { message ->
                                viewModel.onIntent(BlockchainCardIntent.WebMessageReceived(message))
                            },
                        )
                    }
                }
            }

            bottomSheet(BlockchainCardDestination.ManageCardDetailsDestination) {
                state?.currentCard?.let { card ->
                    ManageCardDetails(
                        last4digits = card.last4,
                        cardStatus = card.status,
                        onToggleLockCard = { isChecked: Boolean ->
                            if (isChecked) viewModel.onIntent(BlockchainCardIntent.LockCard)
                            else viewModel.onIntent(BlockchainCardIntent.UnlockCard)
                        },
                        onChangePin = {
                            viewModel.onIntent(BlockchainCardIntent.SetPin)
                        },
                        onSeePersonalDetails = { viewModel.onIntent(BlockchainCardIntent.SeePersonalDetails) },
                        onSeeTransactionControls = { viewModel.onIntent(BlockchainCardIntent.SeeTransactionControls) },
                        onSeeSupport = { viewModel.onIntent(BlockchainCardIntent.SeeSupport) },
                        onSeeDocuments = { viewModel.onIntent(BlockchainCardIntent.SeeDocuments) },
                        onTerminateCard = { viewModel.onIntent(BlockchainCardIntent.CloseCard) },
                        onCloseBottomSheet = { viewModel.onIntent(BlockchainCardIntent.HideBottomSheet) }
                    )
                }
            }

            bottomSheet(BlockchainCardDestination.FundingAccountActionsDestination) {
                FundingAccountActionChooser(
                    onAddFunds = { viewModel.onIntent(BlockchainCardIntent.AddFunds) },
                    onChangeAsset = { viewModel.onIntent(BlockchainCardIntent.ChoosePaymentMethod) },
                    onClose = { viewModel.onIntent(BlockchainCardIntent.HideBottomSheet) }
                )
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
                                viewModel.onIntent(BlockchainCardIntent.UpdateAddress(newAddress = newAddress))
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
                state?.currentCard?.last4?.let { last4 ->
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

            composable(BlockchainCardDestination.AllTransactionsDestination) {
                state?.let { state ->
                    if (state.pendingTransactions != null && state.completedTransactionsGroupedByMonth != null) {
                        CardTransactionHistory(
                            pendingTransactions = state.pendingTransactions,
                            completedTransactionsGroupedByMonth = state.completedTransactionsGroupedByMonth,
                            onSeeTransactionDetails = { transaction ->
                                viewModel.onIntent(BlockchainCardIntent.SeeTransactionDetails(transaction))
                            },
                            onRefreshTransactions = {
                                viewModel.onIntent(BlockchainCardIntent.RefreshTransactions)
                            },
                            isTransactionListRefreshing = state.isTransactionListRefreshing,
                            onGetNextPage = {
                                viewModel.onIntent(BlockchainCardIntent.LoadNextTransactionsPage)
                            }
                        )
                    }
                }
            }

            bottomSheet(BlockchainCardDestination.TransactionDetailsDestination) {
                state?.let { state ->
                    if (state.selectedCardTransaction != null && state.currentCard != null) {
                        CardTransactionDetails(
                            cardTransaction = state.selectedCardTransaction,
                            onCloseBottomSheet = {
                                viewModel.onIntent(BlockchainCardIntent.HideBottomSheet)
                            },
                            last4digits = state.currentCard.last4
                        )
                    }
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

            composable(BlockchainCardDestination.CardActivationDestination) {
                state?.cardActivationUrl?.let { cardActivationUrl ->
                    CardActivationPage(
                        cardActivationUrl = cardActivationUrl,
                        onCardActivated = {
                            viewModel.onIntent(BlockchainCardIntent.OnCardActivated)
                        }
                    )
                }
            }

            composable(BlockchainCardDestination.CardActivationSuccessDestination) {
                CardActivationSuccess(
                    onFinish = { viewModel.onIntent(BlockchainCardIntent.OnFinishCardActivation) }
                )
            }

            composable(BlockchainCardDestination.DocumentsDestination) {
                state?.let { state ->
                    Documents(
                        cardStatements = state.cardStatements,
                        legalDocuments = state.legalDocuments,
                        onViewStatement = { statement ->
                            viewModel.onIntent(
                                BlockchainCardIntent.LoadCardStatementUrl(statement.id)
                            )
                        },
                        onViewLegalDocument = { legalDocument ->
                            viewModel.onIntent(
                                BlockchainCardIntent.OpenDocumentUrl(legalDocument.url)
                            )
                        }
                    )
                }
            }

            composable(BlockchainCardDestination.SetPinDestination) {
                SetPinPage(
                    setPinUrl = state?.setPinUrl,
                    onPinSetSuccess = {
                        viewModel.onIntent(BlockchainCardIntent.OnPinSetSuccess)
                    }
                )
            }

            composable(BlockchainCardDestination.SetPinSuccessDestination) {
                SetPinSuccess(
                    onFinish = {
                        viewModel.onIntent(BlockchainCardIntent.OnFinishSetPin)
                    }
                )
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

    Scaffold(scaffoldState = scaffoldState, content = content, backgroundColor = AppTheme.colors.background)
}
