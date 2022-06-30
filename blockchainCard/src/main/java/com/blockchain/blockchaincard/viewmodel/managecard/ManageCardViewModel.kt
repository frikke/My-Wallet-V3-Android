package com.blockchain.blockchaincard.viewmodel.managecard

import com.blockchain.blockchaincard.domain.BlockchainCardRepository
import com.blockchain.blockchaincard.viewmodel.BlockchainCardArgs
import com.blockchain.blockchaincard.viewmodel.BlockchainCardIntent
import com.blockchain.blockchaincard.viewmodel.BlockchainCardModelState
import com.blockchain.blockchaincard.viewmodel.BlockchainCardNavigationEvent
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewModel
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewState
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
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
                updateState { it.copy(cardProduct = args.product) }
            }
        }
    }

    override fun reduce(state: BlockchainCardModelState): BlockchainCardViewState = BlockchainCardViewState(
        card = state.card,
        cardProduct = state.cardProduct,
        cardWidgetUrl = state.cardWidgetUrl,
        eligibleTradingAccountBalances = state.eligibleTradingAccountBalances,
        isLinkedAccountBalanceLoading = state.isLinkedAccountBalanceLoading,
        linkedAccountBalance = state.linkedAccountBalance,
        residentialAddress = state.residentialAddress,
        userFirstAndLastName = state.userFirstAndLastName,
        transactionList = state.transactionList,
        selectedCardTransaction = state.selectedCardTransaction,
        isTransactionListRefreshing = state.isTransactionListRefreshing,
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
                        onFailure = {
                            Timber.d("Card widget url failed: $it") // TODO(labreu): handle error
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
                        onFailure = {
                            Timber.e("fetch eligible accounts failed: $it") // TODO(labreu): handle error
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
                                    Timber.e("Unable to get asset: $it")
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
                        onFailure = {
                            Timber.e("Account linking failed: $it")
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
                        onFailure = {
                            Timber.e("Unable to get current linked account: $it")
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
                    onFailure = {
                        updateState { it.copy(isLinkedAccountBalanceLoading = false) }
                        Timber.e("Load Account balance failed: $it")
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
                        onFailure = {
                            Timber.e("Load Account balance failed: $it")
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
                    onFailure = {
                        Timber.e("Unable to get residential address: $it")
                    }
                )
            }

            is BlockchainCardIntent.SeeBillingAddress -> {
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
                    onFailure = {
                        Timber.e("Unable to update residential address: $it")
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
                        onFailure = {
                            Timber.d("Card delete failed: $it")
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
                    onFailure = {
                        Timber.e("Unable to get user first and last name: $it")
                    }
                )
            }

            is BlockchainCardIntent.LoadTransactions -> {
                blockchainCardRepository.getTransactions().fold(
                    onSuccess = { transactions ->
                        Timber.d("Transactions loaded: $transactions")
                        updateState { it.copy(transactionList = transactions, isTransactionListRefreshing = false) }
                    },
                    onFailure = {
                        Timber.e("Unable to get transactions: $it")
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
        }
    }
}
