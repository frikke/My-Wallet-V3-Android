package com.blockchain.blockchaincard.viewmodel.ordercard

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

class OrderCardViewModel(private val blockchainCardRepository: BlockchainCardRepository) : BlockchainCardViewModel() {

    override fun viewCreated(args: ModelConfigArgs) {
        when (args) {
            is BlockchainCardArgs.CardArgs -> {
                updateState { it.copy(card = args.card) }
            }

            is BlockchainCardArgs.ProductArgs -> {
                updateState { it.copy(cardProduct = args.product) }
            }
        }
    }

    override fun reduce(state: BlockchainCardModelState): BlockchainCardViewState = BlockchainCardViewState(
        card = state.card,
        cardProduct = state.cardProduct
    )

    override suspend fun handleIntent(
        modelState: BlockchainCardModelState,
        intent: BlockchainCardIntent
    ) {
        when (intent) {
            is BlockchainCardIntent.OrderCard -> {
                navigate(BlockchainCardNavigationEvent.SelectCardForOrder)
            }

            is BlockchainCardIntent.OnSeeProductDetails -> {
                modelState.cardProduct?.let {
                    navigate(BlockchainCardNavigationEvent.SeeProductDetails)
                } ?: Timber.w("Unable to show product details, no product info")
            }

            is BlockchainCardIntent.CreateCard -> {
                navigate(BlockchainCardNavigationEvent.CreateCardInProgress)
                blockchainCardRepository.createCard(productCode = intent.productCode, ssn = intent.ssn).fold(
                    onFailure = {
                        // Todo update state's error here OR pass it to the destination
                        navigate(BlockchainCardNavigationEvent.CreateCardFailed)
                    },
                    onSuccess = { card ->
                        updateState { it.copy(card = card) }
                        navigate(BlockchainCardNavigationEvent.CreateCardSuccess)
                    }
                )
            }

            is BlockchainCardIntent.HideProductDetailsBottomSheet -> {
                navigate(BlockchainCardNavigationEvent.HideBottomSheet)
            }

            is BlockchainCardIntent.ManageCard -> {
                modelState.card?.let {
                    navigate(BlockchainCardNavigationEvent.ManageCard(modelState.card))
                }
            }
        }
    }
}
