package com.blockchain.nfts.detail

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.nfts.detail.navigation.NftDetailNavigationEvent
import com.blockchain.nfts.domain.service.NftService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NftDetailViewModel(
    private val nftService: NftService
) : MviViewModel<NftDetailIntent,
    NftDetailViewState,
    NftDetailModelState,
    NftDetailNavigationEvent,
    NftDetailNavArgs>(
    initialState = NftDetailModelState()
) {
    override fun viewCreated(args: NftDetailNavArgs) {
        loadNftAsset(nftId = args.nftId)
    }

    override fun reduce(state: NftDetailModelState): NftDetailViewState = state.run {
        NftDetailViewState(
            asset = asset
        )
    }

    override suspend fun handleIntent(modelState: NftDetailModelState, intent: NftDetailIntent) {
    }

    private fun loadNftAsset(nftId: String) {
        viewModelScope.launch {
            nftService.getNftAsset(address = "0x5D70101143BF7bbc889D757613e2B2761bD447EC", nftId =  nftId)
                .collectLatest { dataResource ->
                    updateState {
                        it.copy(
                            asset = if (dataResource is DataResource.Loading && it.asset is DataResource.Data) {
                                // if data is present already - don't show loading
                                it.asset
                            } else {
                                dataResource
                            }
                        )
                    }
                }
        }
    }
}
