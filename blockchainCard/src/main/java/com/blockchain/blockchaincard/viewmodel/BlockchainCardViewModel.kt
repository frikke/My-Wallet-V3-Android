package com.blockchain.blockchaincard.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel

class BlockchainCardViewModel(initialState: BlockchainCardModelState) :
    MviViewModel<
        BlockchainCardIntent,
        BlockchainCardViewState,
        BlockchainCardModelState,
        BlockchainCardNavigationEvent,
        ModelConfigArgs> (initialState) {

    override fun viewCreated(args: ModelConfigArgs) {
        when (args) {
            is ModelConfigArgs.NoArgs -> {
                updateState { BlockchainCardModelState.NotOrdered }
            }
            is ModelConfigArgs.ParcelableArgs -> TODO()
        }
    }

    override fun reduce(state: BlockchainCardModelState): BlockchainCardViewState =
        when (state) {
            is BlockchainCardModelState.NotOrdered -> {
                BlockchainCardViewState.OrderCard
            }
            is BlockchainCardModelState.Created -> BlockchainCardViewState.ManageCard(state.card)
            is BlockchainCardModelState.Unknown -> BlockchainCardViewState.OrderCard
        }

    override suspend fun handleIntent(modelState: BlockchainCardModelState, intent: BlockchainCardIntent) {
        TODO("Not yet implemented")
    }
}