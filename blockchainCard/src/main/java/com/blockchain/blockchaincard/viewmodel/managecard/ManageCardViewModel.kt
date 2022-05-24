package com.blockchain.blockchaincard.viewmodel.managecard

import androidx.lifecycle.viewModelScope
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
import com.blockchain.outcome.map
import com.blockchain.outcome.mapLeft
import kotlinx.coroutines.launch
import timber.log.Timber

class ManageCardViewModel(private val blockchainCardRepository: BlockchainCardRepository) : BlockchainCardViewModel() {

    override fun viewCreated(args: ModelConfigArgs) {
        when (args) {
            is BlockchainCardArgs.CardArgs -> {
                updateState { it.copy(card = args.card) }
                onIntent(BlockchainCardIntent.LoadCardWidget)
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
        eligibleTradingAccounts = state.eligibleTradingAccounts
    )

    override suspend fun handleIntent(
        modelState: BlockchainCardModelState,
        intent: BlockchainCardIntent
    ) {
        when (intent) {
            is BlockchainCardIntent.ManageCardDetails -> {
                navigate(BlockchainCardNavigationEvent.ManageCardDetails)
            }

            is BlockchainCardIntent.DeleteCard -> {
                modelState.card?.let { card ->
                    blockchainCardRepository.deleteCard(card.id).fold(
                        onFailure = {
                            Timber.d("Card delete FAILED")
                        },
                        onSuccess = {
                            Timber.d("Card deleted")
                            // Todo should go back to settings
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
                            Timber.d("Card widget url FAILED: $it") // TODO(labreu): handle error
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
                    ).mapLeft {
                        Timber.d("fetch eligible accounts failed: $it") // TODO(labreu): handle error
                    }.map { eligibleAccounts ->
                        val eligibleAccountsWithoutBalance = modelState.eligibleTradingAccounts
                        eligibleAccounts.map { tradingAccount ->
                            eligibleAccountsWithoutBalance[tradingAccount] = null
                            viewModelScope.launch {
                                val eligibleTradingAccounts = modelState.eligibleTradingAccounts
                                blockchainCardRepository.loadAccountBalance(tradingAccount as BlockchainAccount).fold(
                                    onSuccess = { balance ->
                                        eligibleTradingAccounts[tradingAccount] = balance
                                        updateState { it.copy(eligibleTradingAccounts = eligibleTradingAccounts) }
                                    },
                                    onFailure = {
                                        Timber.e("Load Account balance failed")
                                    }
                                )
                            }
                        }

                        updateState { it.copy(eligibleTradingAccounts = eligibleAccountsWithoutBalance) }
                        navigate(BlockchainCardNavigationEvent.ChoosePaymentMethod)
                    }
                }
            }
        }
    }
}
