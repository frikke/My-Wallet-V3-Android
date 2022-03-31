package com.blockchain.blockchaincard.viewmodel

import com.blockchain.blockchaincard.domain.BlockchainCardRepository
import com.blockchain.blockchaincard.domain.models.BlockchainDebitCardProduct
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationEvent
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.parcelize.Parcelize
import timber.log.Timber

sealed class BlockchainDebitCardArgs : ModelConfigArgs.ParcelableArgs {
    @Parcelize
    data class CardArgs(val cardId: String) : ModelConfigArgs.ParcelableArgs

    @Parcelize
    data class ProductArgs(val product: BlockchainDebitCardProduct) : ModelConfigArgs.ParcelableArgs
}

class BlockchainCardViewModel(private val blockchainCardRepository: BlockchainCardRepository) :
    MviViewModel<
        BlockchainCardIntent,
        BlockchainCardViewState,
        BlockchainCardModelState,
        ComposeNavigationEvent,
        ModelConfigArgs> (BlockchainCardModelState()) {

    override fun viewCreated(args: ModelConfigArgs) {
        when (args) {
            is BlockchainDebitCardArgs.CardArgs -> {
                updateState { it.copy(cardId = args.cardId) }
            }

            is BlockchainDebitCardArgs.ProductArgs -> {
                updateState { it.copy(cardProduct = args.product) }
            }
        }
    }

    override fun reduce(state: BlockchainCardModelState): BlockchainCardViewState = BlockchainCardViewState(
        cardId = state.cardId,
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
                    navigate(BlockchainCardNavigationEvent.OnSeeProductDetails)
                } ?: Timber.w("Unable to show product details, no product info")
            }

            is BlockchainCardIntent.CreateCard -> {
                blockchainCardRepository.createCard(productCode = intent.productCode, ssn = intent.ssn)
                    .doOnSubscribe {
                        navigate(BlockchainCardNavigationEvent.CreateCardInProgressDestination)
                    }.subscribeBy(
                        onSuccess = { card ->
                            updateState { it.copy(cardId = card.cardId) }
                            navigate(BlockchainCardNavigationEvent.CreateCardSuccessDestination)
                        },
                        onError = {
                            // Todo update state's error here OR pass it to the destination
                            navigate(BlockchainCardNavigationEvent.CreateCardFailedDestination)
                        }
                    )
            }

            is BlockchainCardIntent.HideProductDetailsBottomSheet -> {
                navigate(BlockchainCardNavigationEvent.HideBottomSheet)
            }

            is BlockchainCardIntent.DeleteCard -> {
                modelState.cardId?.let { cardId ->
                    blockchainCardRepository.deleteCard(cardId).subscribeBy(
                        onSuccess = {
                            Timber.d("Card deleted")
                        },
                        onError = {
                            Timber.d("Card delete FAILED")
                        }
                    )
                }
            }

            is BlockchainCardIntent.ManageCard -> {
                navigate(BlockchainCardNavigationEvent.ManageCardDestination)
            }
        }
    }
}
