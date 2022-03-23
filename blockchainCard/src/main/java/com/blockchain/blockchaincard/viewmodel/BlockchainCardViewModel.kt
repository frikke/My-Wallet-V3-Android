package com.blockchain.blockchaincard.viewmodel

import com.blockchain.blockchaincard.data.BcCardDataRepository
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationEvent
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.parcelize.Parcelize

@Parcelize
data class BlockchainDebitCardArgs(val cardId: String) : ModelConfigArgs.ParcelableArgs

class BlockchainCardViewModel(private val bcCardDataRepository: BcCardDataRepository) :
    MviViewModel<
        BlockchainCardIntent,
        BlockchainCardViewState,
        BlockchainCardModelState,
        ComposeNavigationEvent,
        ModelConfigArgs> (BlockchainCardModelState.Unknown) {

    override fun viewCreated(args: ModelConfigArgs) {
        when (args) {
            is ModelConfigArgs.NoArgs -> {
                updateState { BlockchainCardModelState.NotOrdered }
            }
            is BlockchainDebitCardArgs -> {
                updateState { BlockchainCardModelState.Created(args.cardId)}
            }
        }
    }

    override fun reduce(state: BlockchainCardModelState): BlockchainCardViewState =
        when (state) {
            is BlockchainCardModelState.NotOrdered -> {
                BlockchainCardViewState.OrderOrLinkCard
            }
            is BlockchainCardModelState.OrderCard -> {
                BlockchainCardViewState.OrderCard
            }
            is BlockchainCardModelState.LinkCard -> {
                BlockchainCardViewState.LinkCard
            }

            is BlockchainCardModelState.CardCreationSuccess -> {
                BlockchainCardViewState.CardCreationSuccess
            }

            is BlockchainCardModelState.CardCreationFailed -> {
                BlockchainCardViewState.CardCreationFailed
            }
            is BlockchainCardModelState.Created -> BlockchainCardViewState.ManageCard(state.cardId)
            is BlockchainCardModelState.Unknown -> BlockchainCardViewState.OrderCard
        }

    override suspend fun handleIntent(
        modelState: BlockchainCardModelState,
        intent: BlockchainCardIntent
    ) {
        when (intent) {
            is BlockchainCardIntent.OrderCard -> {
                navigate(BlockchainCardNavigationEvent.SelectCardForOrder)
            }

            is BlockchainCardIntent.CreateCard -> {
                bcCardDataRepository.createCard(productCode = intent.productCode, ssn = intent.ssn).subscribeBy(
                    onSuccess = {
                        updateState { BlockchainCardModelState.CardCreationSuccess }
                        navigate(BlockchainCardNavigationEvent.CreateCardSuccessDestination)
                    },
                    onError = {
                        updateState { BlockchainCardModelState.CardCreationFailed }
                        navigate(BlockchainCardNavigationEvent.CreateCardFailedDestination)
                    }
                )
            }
        }

    }
}