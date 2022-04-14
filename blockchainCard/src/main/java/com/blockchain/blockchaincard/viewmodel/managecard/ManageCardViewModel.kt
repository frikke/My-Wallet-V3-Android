package com.blockchain.blockchaincard.viewmodel.managecard

import com.blockchain.blockchaincard.domain.BlockchainCardRepository
import com.blockchain.blockchaincard.viewmodel.BlockchainCardArgs
import com.blockchain.blockchaincard.viewmodel.BlockchainCardIntent
import com.blockchain.blockchaincard.viewmodel.BlockchainCardModelState
import com.blockchain.blockchaincard.viewmodel.BlockchainCardNavigationEvent
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewModel
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewState
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.outcome.fold
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
        cardWidgetUrl = state.cardWidgetUrl
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
        }
    }
}
