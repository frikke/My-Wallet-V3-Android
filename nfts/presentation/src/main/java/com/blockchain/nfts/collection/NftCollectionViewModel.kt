package com.blockchain.nfts.collection

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.nfts.collection.navigation.NftCollectionNavigationEvent
import com.blockchain.nfts.domain.service.NftService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
        loadNftCollection()
    }

    override fun reduce(state: NftCollectionModelState): NftCollectionViewState {
        return with(state) {
            NftCollectionViewState()
        }
    }

    override suspend fun handleIntent(modelState: NftCollectionModelState, intent: NftCollectionIntent) {
    }

    private fun loadNftCollection() {
        viewModelScope.launch {
            nftService.getNftForAddress(address = "0x5D70101143BF7bbc889D757613e2B2761bD447EC")
                .collectLatest {

                }
        }
    }
}
