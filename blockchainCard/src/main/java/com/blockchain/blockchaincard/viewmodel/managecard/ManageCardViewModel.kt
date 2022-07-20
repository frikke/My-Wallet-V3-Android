package com.blockchain.blockchaincard.viewmodel.managecard

import com.blockchain.blockchaincard.domain.BlockchainCardRepository
import com.blockchain.blockchaincard.viewmodel.BlockchainCardArgs
import com.blockchain.blockchaincard.viewmodel.BlockchainCardErrorState
import com.blockchain.blockchaincard.viewmodel.BlockchainCardIntent
import com.blockchain.blockchaincard.viewmodel.BlockchainCardModelState
import com.blockchain.blockchaincard.viewmodel.BlockchainCardNavigationEvent
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewModel
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewState
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.fold
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import timber.log.Timber

class ManageCardViewModel(private val blockchainCardRepository: BlockchainCardRepository) : BlockchainCardViewModel() {

    override fun viewCreated(args: ModelConfigArgs) {
        when (args) {
            is BlockchainCardArgs.CardArgs -> {
                updateState { it.copy(card = args.card) }
                onIntent(BlockchainCardIntent.LoadCardWidget)
                onIntent(BlockchainCardIntent.LoadLinkedAccount)
                onIntent(BlockchainCardIntent.LoadTransactions)
            }

            is BlockchainCardArgs.ProductArgs -> {
                updateState { it.copy(selectedCardProduct = args.product) }
            }
        }
    }

    override fun reduce(state: BlockchainCardModelState): BlockchainCardViewState = BlockchainCardViewState(
        errorState = state.errorState,
        card = state.card,
        selectedCardProduct = state.selectedCardProduct,
        cardWidgetUrl = state.cardWidgetUrl,
        eligibleTradingAccountBalances = state.eligibleTradingAccountBalances,
        isLinkedAccountBalanceLoading = state.isLinkedAccountBalanceLoading,
        linkedAccountBalance = state.linkedAccountBalance,
        residentialAddress = state.residentialAddress,
        userFirstAndLastName = state.userFirstAndLastName,
        transactionList = state.transactionList,
        selectedCardTransaction = state.selectedCardTransaction,
        isTransactionListRefreshing = state.isTransactionListRefreshing,
        countryStateList = state.countryStateList,
    )

    override suspend fun handleIntent(
        modelState: BlockchainCardModelState,
        intent: BlockchainCardIntent
    ) {
        when (intent) {
            is BlockchainCardIntent.ManageCardDetails -> {
                navigate(BlockchainCardNavigationEvent.ManageCardDetails)
            }

            is BlockchainCardIntent.LockCard -> {
                modelState.card?.let { card ->
                    blockchainCardRepository.lockCard(card.id).fold(
                        onFailure = { error ->
                            Timber.e("Card lock failed: $error")
                            updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                        },
                        onSuccess = { cardUpdated ->
                            Timber.d("Card locked")
                            updateState { it.copy(card = cardUpdated) }
                        }
                    )
                }
            }

            is BlockchainCardIntent.UnlockCard -> {
                modelState.card?.let { card ->
                    blockchainCardRepository.unlockCard(card.id).fold(
                        onFailure = { error ->
                            Timber.e("Card unlock failed: $error")
                            updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                        },
                        onSuccess = { cardUpdated ->
                            Timber.d("Card unlocked")
                            updateState { it.copy(card = cardUpdated) }
                        }
                    )
                }
            }

            is BlockchainCardIntent.LoadCardWidget -> {
                modelState.card?.let { card ->
                    blockchainCardRepository.getCardWidgetUrl(
                        cardId = card.id,
                        last4Digits = card.last4
                    ).fold(
                        onFailure = { error ->
                            Timber.d("Card widget url failed: $error")
                            updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                        },
                        onSuccess = { cardWidgetUrl ->
                            updateState { it.copy(cardWidgetUrl = cardWidgetUrl) }
                        }
                    )
                }
            }

            is BlockchainCardIntent.ChoosePaymentMethod -> {
                modelState.card?.let { card ->
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

            is BlockchainCardIntent.TopUp -> {
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
                modelState.card?.let { card ->
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
                modelState.card?.let { card ->
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
                modelState.eligibleTradingAccountBalances.clear()
                intent.eligibleAccounts.map { tradingAccount ->
                    blockchainCardRepository.loadAccountBalance(
                        tradingAccount as BlockchainAccount
                    ).fold(
                        onSuccess = { balance ->
                            val eligibleTradingAccountBalances = modelState.eligibleTradingAccountBalances
                            eligibleTradingAccountBalances.add(balance)
                            updateState {
                                it.copy(eligibleTradingAccountBalances = eligibleTradingAccountBalances)
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
                onIntent(BlockchainCardIntent.LoadUserFirstAndLastName)
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
                }
                navigate(BlockchainCardNavigationEvent.SeeBillingAddress)
            }

            is BlockchainCardIntent.UpdateBillingAddress -> {
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
                modelState.card?.let { card ->
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
                blockchainCardRepository.getTransactions().fold(
                    onSuccess = { transactions ->
                        Timber.d("Transactions loaded: $transactions")
                        updateState { it.copy(transactionList = transactions, isTransactionListRefreshing = false) }
                    },
                    onFailure = { error ->
                        Timber.e("Unable to get transactions: $error")
                        updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                    }
                )
            }

            is BlockchainCardIntent.RefreshTransactions -> {
                updateState { it.copy(isTransactionListRefreshing = true) }
                onIntent(BlockchainCardIntent.LoadTransactions)
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
        }
    }
}
