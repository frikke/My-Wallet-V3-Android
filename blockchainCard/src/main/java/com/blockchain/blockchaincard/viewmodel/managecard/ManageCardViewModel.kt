package com.blockchain.blockchaincard.viewmodel.managecard

import com.blockchain.blockchaincard.domain.BlockchainCardRepository
import com.blockchain.blockchaincard.domain.models.BlockchainCardGoogleWalletData
import com.blockchain.blockchaincard.domain.models.BlockchainCardGoogleWalletStatus
import com.blockchain.blockchaincard.domain.models.BlockchainCardPostMessageType
import com.blockchain.blockchaincard.domain.models.BlockchainCardStatus
import com.blockchain.blockchaincard.domain.models.BlockchainCardTransactionState
import com.blockchain.blockchaincard.util.BlockchainCardTransactionUtils
import com.blockchain.blockchaincard.util.getCompletedTransactionsGroupedByMonth
import com.blockchain.blockchaincard.util.getPendingTransactions
import com.blockchain.blockchaincard.viewmodel.BlockchainCardArgs
import com.blockchain.blockchaincard.viewmodel.BlockchainCardErrorState
import com.blockchain.blockchaincard.viewmodel.BlockchainCardIntent
import com.blockchain.blockchaincard.viewmodel.BlockchainCardModelState
import com.blockchain.blockchaincard.viewmodel.BlockchainCardNavigationEvent
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewModel
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewState
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.fold
import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.getMonthName
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import timber.log.Timber

const val TRANSACTIONS_PAGE_SIZE = 30

class ManageCardViewModel(private val blockchainCardRepository: BlockchainCardRepository) : BlockchainCardViewModel() {

    override fun viewCreated(args: ModelConfigArgs) {
        when (args) {
            is BlockchainCardArgs.CardArgs -> {
                if (args.preselectedCard != null) {
                    updateState {
                        it.copy(
                            cardList = args.cards,
                            cardProductList = args.cardProducts,
                            currentCard = args.preselectedCard,
                            defaultCardId = args.preselectedCard.id,
                        )
                    }

                    if (args.preselectedCard.status != BlockchainCardStatus.TERMINATED) {
                        onIntent(BlockchainCardIntent.LoadCardWidget)
                        onIntent(BlockchainCardIntent.LoadGoogleWalletTokenizationStatus)
                    }

                    onIntent(BlockchainCardIntent.LoadUserFirstAndLastName)
                    onIntent(BlockchainCardIntent.LoadLinkedAccount)
                    onIntent(BlockchainCardIntent.LoadTransactions)
                } else if (args.cards.filter { it.status != BlockchainCardStatus.TERMINATED }.size == 1) {
                    updateState {
                        it.copy(
                            cardList = args.cards,
                            cardProductList = args.cardProducts,
                            currentCard = args.cards.first { card -> card.status != BlockchainCardStatus.TERMINATED },
                        )
                    }

                    onIntent(BlockchainCardIntent.LoadCardWidget)
                    onIntent(BlockchainCardIntent.LoadGoogleWalletTokenizationStatus)
                    onIntent(BlockchainCardIntent.LoadUserFirstAndLastName)
                    onIntent(BlockchainCardIntent.LoadLinkedAccount)
                    onIntent(BlockchainCardIntent.LoadTransactions)
                } else {
                    updateState {
                        it.copy(
                            cardList = args.cards,
                            cardProductList = args.cardProducts,
                        )
                    }
                }
            }

            else -> {
                throw IllegalStateException("ManageCardViewModel the provided arguments are not valid")
            }
        }
    }

    override fun reduce(state: BlockchainCardModelState): BlockchainCardViewState = BlockchainCardViewState(
        errorState = state.errorState,
        cardList = state.cardList,
        currentCard = state.currentCard,
        defaultCardId = state.defaultCardId,
        selectedCardProduct = state.selectedCardProduct,
        cardWidgetUrl = state.cardWidgetUrl,
        eligibleTradingAccountBalances = state.eligibleTradingAccountBalances,
        isLinkedAccountBalanceLoading = state.isLinkedAccountBalanceLoading,
        linkedAccountBalance = state.linkedAccountBalance,
        residentialAddress = state.residentialAddress,
        userFirstAndLastName = state.userFirstAndLastName,
        shortTransactionList = state.shortTransactionList,
        pendingTransactions = state.pendingTransactions,
        completedTransactionsGroupedByMonth = state.completedTransactionsGroupedByMonth,
        selectedCardTransaction = state.selectedCardTransaction,
        isTransactionListRefreshing = state.isTransactionListRefreshing,
        countryStateList = state.countryStateList,
        googleWalletStatus = state.googleWalletStatus,
        cardOrderState = state.cardOrderState,
        cardActivationUrl = state.cardActivationUrl,
        cardStatements = state.cardStatements,
        legalDocuments = state.legalDocuments
    )

    override suspend fun handleIntent(
        modelState: BlockchainCardModelState,
        intent: BlockchainCardIntent
    ) {
        when (intent) {
            is BlockchainCardIntent.ManageCard -> {
                updateState { it.copy(currentCard = intent.card) }

                if (intent.card.status != BlockchainCardStatus.TERMINATED) {
                    onIntent(BlockchainCardIntent.LoadCardWidget)
                    onIntent(BlockchainCardIntent.LoadGoogleWalletTokenizationStatus)

                    if (intent.card.status == BlockchainCardStatus.UNACTIVATED) {
                        onIntent(BlockchainCardIntent.LoadCardOrderState)
                    }
                }

                onIntent(BlockchainCardIntent.LoadUserFirstAndLastName)
                onIntent(BlockchainCardIntent.LoadLinkedAccount)
                onIntent(BlockchainCardIntent.LoadTransactions)

                navigate(BlockchainCardNavigationEvent.ManageCard)
            }

            is BlockchainCardIntent.SelectCard -> {
                navigate(
                    BlockchainCardNavigationEvent.ViewCardSelector(
                        hasDefault = modelState.defaultCardId.isNotEmpty()
                    )
                )
            }

            is BlockchainCardIntent.LoadDefaultCard -> {
                val defaultCardId = blockchainCardRepository.getDefaultCard()
                if (defaultCardId.isNotEmpty()) {
                    updateState { it.copy(defaultCardId = defaultCardId) }
                }
            }

            is BlockchainCardIntent.SaveCardAsDefault -> {
                blockchainCardRepository.saveCardAsDefault(intent.defaultCardId)
                updateState { it.copy(defaultCardId = intent.defaultCardId) }
            }

            is BlockchainCardIntent.LoadCards -> {
                blockchainCardRepository.getCards().fold(
                    onSuccess = { cards ->
                        updateState { it.copy(cardList = cards) }
                    },
                    onFailure = { error ->
                        Timber.e("Unable to refresh card list")
                        updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                    }
                )
            }

            is BlockchainCardIntent.OrderCard -> {
                navigate(BlockchainCardNavigationEvent.OrderCard)
            }

            is BlockchainCardIntent.ManageCardDetails -> {
                updateState { it.copy(currentCard = intent.card) }
                navigate(BlockchainCardNavigationEvent.ManageCardDetails)
            }

            is BlockchainCardIntent.LockCard -> {
                modelState.currentCard?.let { card ->
                    blockchainCardRepository.lockCard(card.id).fold(
                        onFailure = { error ->
                            Timber.e("Card lock failed: $error")
                            updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                        },
                        onSuccess = { cardUpdated ->
                            Timber.d("Card locked")
                            updateState { it.copy(currentCard = cardUpdated) }
                        }
                    )
                }
            }

            is BlockchainCardIntent.UnlockCard -> {
                modelState.currentCard?.let { card ->
                    blockchainCardRepository.unlockCard(card.id).fold(
                        onFailure = { error ->
                            Timber.e("Card unlock failed: $error")
                            updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                        },
                        onSuccess = { cardUpdated ->
                            Timber.d("Card unlocked")
                            updateState { it.copy(currentCard = cardUpdated) }
                        }
                    )
                }
            }

            is BlockchainCardIntent.LoadCardWidget -> {
                updateState { it.copy(cardWidgetUrl = null) }
                if (modelState.currentCard != null) {
                    blockchainCardRepository.getUserFirstAndLastName().flatMap { firstAndLastName ->
                        blockchainCardRepository.getCardWidgetUrl(
                            cardId = modelState.currentCard.id,
                            last4Digits = modelState.currentCard.last4,
                            userFullName = firstAndLastName
                        )
                    }.fold(
                        onFailure = { error ->
                            Timber.d("Card widget url failed: $error")
                            updateState {
                                it.copy(
                                    errorState = BlockchainCardErrorState.SnackbarErrorState(error),
                                    cardWidgetUrl = ""
                                )
                            }
                        },
                        onSuccess = { cardWidgetUrl ->
                            updateState { it.copy(cardWidgetUrl = cardWidgetUrl) }
                        }
                    )
                }
            }

            is BlockchainCardIntent.FundingAccountClicked -> {
                navigate(BlockchainCardNavigationEvent.ChooseFundingAccountAction)
            }

            is BlockchainCardIntent.ChoosePaymentMethod -> {
                modelState.currentCard?.let { card ->
                    blockchainCardRepository.getEligibleTradingAccounts(
                        cardId = card.id
                    ).fold(
                        onSuccess = { eligibleAccounts ->
                            onIntent(BlockchainCardIntent.LoadEligibleAccountsBalances(eligibleAccounts))
                            navigate(BlockchainCardNavigationEvent.ChoosePaymentMethod)
                        },
                        onFailure = { error ->
                            Timber.e("fetch eligible accounts failed: $error")
                            updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                        }
                    )
                }
            }

            is BlockchainCardIntent.AddFunds -> {
                modelState.linkedAccountBalance?.let { accountBalance ->
                    when (accountBalance.total) {
                        is FiatValue -> {
                            blockchainCardRepository.getFiatAccount(
                                accountBalance.totalFiat.currency.networkTicker
                            ).fold(
                                onSuccess = { account ->
                                    navigate(BlockchainCardNavigationEvent.TopUpFiat(account))
                                },
                                onFailure = {
                                    Timber.e("Unable to get fiat account: $it")
                                }
                            )
                        }
                        is CryptoValue -> {
                            blockchainCardRepository.getAsset(
                                accountBalance.total.currency.networkTicker
                            ).fold(
                                onSuccess = { asset ->
                                    navigate(BlockchainCardNavigationEvent.TopUpCrypto(asset))
                                },
                                onFailure = {
                                    Timber.e("Unable to get asset")
                                }
                            )
                        }
                        else ->
                            throw IllegalStateException("Unable to top up, current asset is not Fiat nor Crypto value")
                    }
                }
            }

            is BlockchainCardIntent.LinkSelectedAccount -> {
                modelState.currentCard?.let { card ->
                    blockchainCardRepository.linkCardAccount(
                        cardId = card.id,
                        accountCurrency = intent.accountCurrencyNetworkTicker
                    ).fold(
                        onSuccess = {
                            onIntent(BlockchainCardIntent.LoadLinkedAccount)
                            navigate(BlockchainCardNavigationEvent.HideBottomSheet)
                        },
                        onFailure = { error ->
                            Timber.e("Account linking failed: $error")
                            updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                        }
                    )
                }
            }

            is BlockchainCardIntent.LoadLinkedAccount -> {
                modelState.currentCard?.let { card ->
                    updateState { it.copy(isLinkedAccountBalanceLoading = true) }
                    blockchainCardRepository.getCardLinkedAccount(
                        cardId = card.id
                    ).fold(
                        onSuccess = { linkedTradingAccount ->
                            onIntent(BlockchainCardIntent.LoadAccountBalance(linkedTradingAccount as BlockchainAccount))
                        },
                        onFailure = { error ->
                            Timber.e("Unable to get current linked account: $error")
                            updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                        }
                    )
                }
            }

            is BlockchainCardIntent.LoadAccountBalance -> {
                blockchainCardRepository.loadAccountBalance(
                    intent.tradingAccount
                ).fold(
                    onSuccess = { balance ->
                        updateState {
                            it.copy(linkedAccountBalance = balance, isLinkedAccountBalanceLoading = false)
                        }
                    },
                    onFailure = { error ->
                        Timber.e("Load Account balance failed: $error")
                        updateState {
                            it.copy(
                                isLinkedAccountBalanceLoading = false,
                                errorState = BlockchainCardErrorState.SnackbarErrorState(error)
                            )
                        }
                    }
                )
            }

            is BlockchainCardIntent.LoadEligibleAccountsBalances -> {
                updateState { it.copy(eligibleTradingAccountBalances = emptyList()) }
                val eligibleTradingAccountBalancesMutable = mutableListOf<AccountBalance>()

                intent.eligibleAccounts.map { tradingAccount ->
                    blockchainCardRepository.loadAccountBalance(
                        tradingAccount as BlockchainAccount
                    ).fold(
                        onSuccess = { balance ->
                            eligibleTradingAccountBalancesMutable.add(balance)
                            if (eligibleTradingAccountBalancesMutable.size == intent.eligibleAccounts.size) {
                                updateState {
                                    it.copy(eligibleTradingAccountBalances = eligibleTradingAccountBalancesMutable)
                                }
                            }
                        },
                        onFailure = { error ->
                            Timber.e("Load Account balance failed: $error")
                            updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                        }
                    )
                }
            }

            is BlockchainCardIntent.SeeTransactionControls -> {
                navigate(BlockchainCardNavigationEvent.SeeTransactionControls)
            }

            is BlockchainCardIntent.SeePersonalDetails -> {
                onIntent(BlockchainCardIntent.LoadResidentialAddress)
                navigate(BlockchainCardNavigationEvent.SeePersonalDetails)
            }

            is BlockchainCardIntent.LoadResidentialAddress -> {
                blockchainCardRepository.getResidentialAddress().fold(
                    onSuccess = { address ->
                        updateState { it.copy(residentialAddress = address) }
                    },
                    onFailure = { error ->
                        Timber.e("Unable to get residential address: $error")
                        updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                    }
                )
            }

            is BlockchainCardIntent.SeeBillingAddress -> {
                modelState.residentialAddress?.let { address ->
                    blockchainCardRepository.getStatesList(address.country)
                        .doOnSuccess { states ->
                            updateState { it.copy(countryStateList = states) }
                        }
                        .doOnFailure {
                            Timber.e("Unable to get states: $it")
                        }
                    navigate(BlockchainCardNavigationEvent.SeeAddress(address))
                }
            }

            is BlockchainCardIntent.UpdateAddress -> {
                blockchainCardRepository.updateResidentialAddress(
                    intent.newAddress
                ).fold(
                    onSuccess = { newAddress ->
                        updateState { it.copy(residentialAddress = newAddress) }
                        navigate(BlockchainCardNavigationEvent.BillingAddressUpdated(success = true))
                    },
                    onFailure = { error ->
                        Timber.e("Unable to update residential address: $error")
                        updateState { it.copy(errorState = BlockchainCardErrorState.ScreenErrorState(error)) }
                        navigate(BlockchainCardNavigationEvent.BillingAddressUpdated(success = false))
                    }
                )
            }

            is BlockchainCardIntent.DismissBillingAddressUpdateResult -> {
                navigate(BlockchainCardNavigationEvent.DismissBillingAddressUpdateResult)
            }

            is BlockchainCardIntent.SeeSupport -> {
                navigate(BlockchainCardNavigationEvent.SeeSupport)
            }

            is BlockchainCardIntent.CloseCard -> {
                navigate(BlockchainCardNavigationEvent.CloseCard)
            }

            is BlockchainCardIntent.ConfirmCloseCard -> {
                modelState.currentCard?.let { card ->
                    blockchainCardRepository.deleteCard(card.id).fold(
                        onFailure = { error ->
                            Timber.d("Card delete failed: $error")
                            updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                        },
                        onSuccess = {
                            Timber.d("Card deleted")
                            navigate(BlockchainCardNavigationEvent.CardClosed)
                        }
                    )
                }
            }

            is BlockchainCardIntent.LoadUserFirstAndLastName -> {
                blockchainCardRepository.getUserFirstAndLastName().fold(
                    onSuccess = { firstAndLastName ->
                        updateState { it.copy(userFirstAndLastName = firstAndLastName) }
                    },
                    onFailure = { error ->
                        Timber.e("Unable to get user first and last name: $error")
                        updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                    }
                )
            }

            is BlockchainCardIntent.LoadTransactions -> {
                blockchainCardRepository.getTransactions(limit = TRANSACTIONS_PAGE_SIZE).fold(
                    onSuccess = { transactions ->
                        Timber.d("Transactions loaded: $transactions")

                        val pendingTransactions = transactions.filter {
                            it.state == BlockchainCardTransactionState.PENDING
                        }
                        val completedTransactionsGroupedByMonth = transactions.filter {
                            it.state != BlockchainCardTransactionState.PENDING
                        }.groupBy {
                            it.userTransactionTime.fromIso8601ToUtc()?.getMonthName()
                        }

                        updateState {
                            it.copy(
                                shortTransactionList = transactions
                                    .filter { txn -> txn.cardId == modelState.currentCard?.id }
                                    .take(4), // We only display the first 4
                                pendingTransactions = pendingTransactions,
                                completedTransactionsGroupedByMonth = completedTransactionsGroupedByMonth,
                                isTransactionListRefreshing = false,
                                nextPageId = if (transactions.isNotEmpty()) transactions.last().id else null
                            )
                        }
                    },
                    onFailure = { error ->
                        Timber.e("Unable to get transactions: $error")
                        updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                    }
                )
            }

            is BlockchainCardIntent.LoadNextTransactionsPage -> {

                if (!modelState.nextPageId.isNullOrEmpty()) {
                    blockchainCardRepository.getTransactions(
                        limit = TRANSACTIONS_PAGE_SIZE,
                        toId = modelState.nextPageId
                    ).fold(
                        onSuccess = { transactions ->
                            Timber.d(
                                "Loaded Next Page. Elements loaded: ${transactions.size}"
                            )

                            val pendingTransactions =
                                transactions.getPendingTransactions()

                            val finalPendingTransactions = BlockchainCardTransactionUtils.mergeTransactionList(
                                firstList = modelState.pendingTransactions,
                                secondList = pendingTransactions
                            )

                            val completedTransactionsGroupedByMonth =
                                transactions.getCompletedTransactionsGroupedByMonth()

                            val finalCompletedTransactionsGroupedByMonth =
                                modelState.completedTransactionsGroupedByMonth?.let {
                                    BlockchainCardTransactionUtils.mergeGroupedTransactionList(
                                        firstGroupedList = it,
                                        secondGroupedList = completedTransactionsGroupedByMonth
                                    )
                                } ?: completedTransactionsGroupedByMonth

                            updateState {
                                it.copy(
                                    pendingTransactions = finalPendingTransactions,
                                    completedTransactionsGroupedByMonth = finalCompletedTransactionsGroupedByMonth,
                                    isTransactionListRefreshing = false,
                                    nextPageId = if (transactions.isNotEmpty()) transactions.last().id else null
                                )
                            }
                        },
                        onFailure = { error ->
                            Timber.e("Unable to get transactions: $error")
                            updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                        }
                    )
                }
            }

            is BlockchainCardIntent.RefreshTransactions -> {
                updateState { it.copy(isTransactionListRefreshing = true) }
                onIntent(BlockchainCardIntent.LoadTransactions)
            }

            is BlockchainCardIntent.SeeAllTransactions -> {
                if (modelState.pendingTransactions != null && modelState.completedTransactionsGroupedByMonth != null) {
                    navigate(BlockchainCardNavigationEvent.SeeAllTransactions)
                }
            }

            is BlockchainCardIntent.SeeTransactionDetails -> {
                updateState { it.copy(selectedCardTransaction = intent.transaction) }
                navigate(BlockchainCardNavigationEvent.SeeTransactionDetails)
            }

            is BlockchainCardIntent.HideBottomSheet -> {
                navigate(BlockchainCardNavigationEvent.HideBottomSheet)
            }

            is BlockchainCardIntent.SnackbarDismissed -> {
                updateState { it.copy(errorState = null) }
            }

            is BlockchainCardIntent.SeeCardLostPage -> {
                navigate(BlockchainCardNavigationEvent.SeeCardLostPage)
            }

            is BlockchainCardIntent.SeeFAQPage -> {
                navigate(BlockchainCardNavigationEvent.SeeFAQPage)
            }

            is BlockchainCardIntent.SeeContactSupportPage -> {
                navigate(BlockchainCardNavigationEvent.SeeContactSupportPage)
            }

            is BlockchainCardIntent.LoadGoogleWalletTokenizationStatus -> {
                modelState.currentCard?.let { card ->
                    blockchainCardRepository.getGoogleWalletTokenizationStatus(card.last4)
                        .doOnSuccess { isTokenized ->
                            if (isTokenized) {
                                Timber.d("Card Already Tokenized")
                                updateState { it.copy(googleWalletStatus = BlockchainCardGoogleWalletStatus.ADDED) }
                            } else {
                                onIntent(BlockchainCardIntent.LoadGoogleWalletDetails)
                            }
                        }
                        .doOnFailure { error ->
                            Timber.e("Unable to get tokenization status")
                            updateState {
                                it.copy(
                                    googleWalletStatus = BlockchainCardGoogleWalletStatus.ADDED,
                                    errorState = BlockchainCardErrorState.SnackbarErrorState(error)
                                )
                            }
                        }
                }
            }

            is BlockchainCardIntent.LoadGoogleWalletDetails -> {
                blockchainCardRepository.getGoogleWalletId()
                    .doOnSuccess { walletId ->
                        updateState { it.copy(googleWalletId = walletId) }
                    }
                    .doOnFailure { error ->
                        Timber.e("Unable to retrieve google wallet id")
                        updateState {
                            it.copy(
                                googleWalletStatus = BlockchainCardGoogleWalletStatus.ADDED,
                                errorState = BlockchainCardErrorState.SnackbarErrorState(error)
                            )
                        }
                    }

                blockchainCardRepository.getGoogleWalletStableHardwareId()
                    .doOnSuccess { stableHardwareId ->
                        updateState { it.copy(stableHardwareId = stableHardwareId) }
                    }
                    .doOnFailure { error ->
                        Timber.e("Unable to retrieve stable hardware id")
                        updateState {
                            it.copy(
                                googleWalletStatus = BlockchainCardGoogleWalletStatus.ADDED,
                                errorState = BlockchainCardErrorState.SnackbarErrorState(error)
                            )
                        }
                    }
            }

            is BlockchainCardIntent.LoadGoogleWalletPushTokenizeData -> {
                if (!modelState.stableHardwareId.isNullOrEmpty() && !modelState.googleWalletId.isNullOrEmpty()) {
                    updateState { it.copy(googleWalletStatus = BlockchainCardGoogleWalletStatus.ADD_IN_PROGRESS) }
                    modelState.currentCard?.let { card ->
                        blockchainCardRepository.provisionGoogleWalletCard(
                            cardId = card.id,
                            provisionRequest = BlockchainCardGoogleWalletData(
                                deviceId = modelState.stableHardwareId,
                                deviceType = "MOBILE_PHONE", // TODO (labreu): hardcoded
                                provisioningAppVersion = "alpha", // TODO (labreu): hardcoded
                                walletAccountId = modelState.googleWalletId
                            )
                        ).doOnSuccess { tokenizeData ->
                            navigate(BlockchainCardNavigationEvent.AddCardToGoogleWallet(tokenizeData))
                        }.doOnFailure { error ->
                            updateState {
                                it.copy(
                                    googleWalletStatus = BlockchainCardGoogleWalletStatus.ADD_FAILED,
                                    errorState = BlockchainCardErrorState.SnackbarErrorState(error)
                                )
                            }
                        }
                    }
                }
            }

            is BlockchainCardIntent.GoogleWalletAddCardFailed -> {
                updateState { it.copy(googleWalletStatus = BlockchainCardGoogleWalletStatus.ADD_FAILED) }
            }

            is BlockchainCardIntent.GoogleWalletAddCardSuccess -> {
                updateState { it.copy(googleWalletStatus = BlockchainCardGoogleWalletStatus.ADD_SUCCESS) }
            }

            is BlockchainCardIntent.LoadCardOrderState -> {
                modelState.currentCard?.let { card ->
                    blockchainCardRepository.getCardOrderState(card.id).fold(
                        onSuccess = { orderState ->
                            updateState { it.copy(cardOrderState = orderState) }
                        },
                        onFailure = { error ->
                            Timber.e("Unable to fetch card order state")
                            updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                        }
                    )
                }
            }

            is BlockchainCardIntent.ActivateCard -> {
                blockchainCardRepository.getCardActivationUrl().fold(
                    onSuccess = { url ->
                        updateState { it.copy(cardActivationUrl = url) }
                    },
                    onFailure = { error ->
                        Timber.e("Unable to fetch card activation URL")
                        updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                    }
                )

                navigate(BlockchainCardNavigationEvent.SeeCardActivationPage)
            }

            is BlockchainCardIntent.OnCardActivated -> {
                modelState.currentCard?.let { card ->
                    // Refresh Card
                    blockchainCardRepository.getCard(card.id).fold(
                        onSuccess = { updatedCard ->
                            updateState { it.copy(currentCard = updatedCard) }
                            navigate(BlockchainCardNavigationEvent.ActivateCardSuccess)
                        },
                        onFailure = { error ->
                            Timber.e("Unable to fetch updated card after activation")
                            updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                        }
                    )
                }
            }

            is BlockchainCardIntent.OnFinishCardActivation -> {
                navigate(BlockchainCardNavigationEvent.ManageCard)
            }

            is BlockchainCardIntent.SeeDocuments -> {
                onIntent(BlockchainCardIntent.LoadCardStatements)
                onIntent(BlockchainCardIntent.LoadLegalDocuments)
                navigate(BlockchainCardNavigationEvent.SeeDocuments)
            }

            is BlockchainCardIntent.LoadCardStatements -> {
                blockchainCardRepository.getCardStatements().fold(
                    onSuccess = { statements ->
                        updateState { it.copy(cardStatements = statements) }
                    },
                    onFailure = { error ->
                        Timber.e("Unable to fetch card statements")
                        updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                    }
                )
            }

            is BlockchainCardIntent.LoadCardStatementUrl -> {
                blockchainCardRepository.getCardStatementUrl(intent.statementId).fold(
                    onSuccess = { url ->
                        onIntent(BlockchainCardIntent.OpenDocumentUrl(url))
                    },
                    onFailure = { error ->
                        Timber.e("Unable to fetch card statement URL")
                        updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                    }
                )
            }

            is BlockchainCardIntent.LoadLegalDocuments -> {
                blockchainCardRepository.getLegalDocuments()
                    .doOnSuccess { documents ->
                        Timber.d("Legal documents loaded: $documents")
                        updateState { it.copy(legalDocuments = documents) }
                    }
                    .doOnFailure { error ->
                        Timber.e("Unable to get legal documents: $error")
                        updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                    }
            }

            is BlockchainCardIntent.OpenDocumentUrl -> {
                navigate(BlockchainCardNavigationEvent.OpenDocumentUrl(intent.url))
            }

            is BlockchainCardIntent.WebMessageReceived -> {
                blockchainCardRepository.decodePostMessageType(intent.message).fold(
                    onSuccess = { postMessageType ->
                        if (postMessageType == BlockchainCardPostMessageType.MANAGE) {
                            modelState.currentCard?.let {
                                onIntent(BlockchainCardIntent.ManageCardDetails(modelState.currentCard))
                            }
                        }
                    },
                    onFailure = { error ->
                        Timber.i("Unable to decode post message type: $error")
                    }
                )
            }

            else -> {
                Timber.e("Unknown intent: $intent")
            }
        }
    }
}
