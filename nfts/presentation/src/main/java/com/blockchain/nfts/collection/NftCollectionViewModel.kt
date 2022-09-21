package com.blockchain.nfts.collection

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.nfts.collection.navigation.NftCollectionNavigationEvent
import com.blockchain.nfts.domain.service.NftService

class NftCollectionViewModel(
    private val nftService: NftService
) : MviViewModel<NftCollectionIntent,
    NftCollectionViewState,
    NftCollectionModelState,
    NftCollectionNavigationEvent,
    ModelConfigArgs.NoArgs>(
    initialState = NftCollectionModelState()
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun reduce(state: NftCollectionModelState): NftCollectionViewState {
        return with(state) {
            NftCollectionViewState()
        }
    }

    override suspend fun handleIntent(modelState: NftCollectionModelState, intent: NftCollectionIntent) {
    }
}
