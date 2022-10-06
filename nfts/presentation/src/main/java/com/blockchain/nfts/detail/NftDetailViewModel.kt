package com.blockchain.nfts.detail

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.nfts.NFT_NETWORK
import com.blockchain.nfts.OPENSEA_ASSET_URL
import com.blockchain.nfts.detail.navigation.NftDetailNavigationEvent
import com.blockchain.nfts.domain.models.NftAsset
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
        loadNftAsset(nftId = args.nftId, args.address)
    }

    override fun reduce(state: NftDetailModelState): NftDetailViewState = state.run {
        NftDetailViewState(
            nftAsset = asset
        )
    }

    override suspend fun handleIntent(modelState: NftDetailModelState, intent: NftDetailIntent) {
        when (intent) {
            is NftDetailIntent.ExternalViewRequested -> {
                navigate(
                    NftDetailNavigationEvent.ExternalView(
                        url = intent.nftAsset.getOpenSeaUrl()
                    )
                )
            }
        }
    }

    private fun loadNftAsset(nftId: String, address: String) {
        viewModelScope.launch {
            nftService.getNftAsset(
                address = address,
                nftId = nftId,
                pageKey = null
            ).collectLatest { dataResource ->
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

    private fun NftAsset.getOpenSeaUrl(): String = run {
        // https://opensea.io/assets/ethereum/0x2809a8737477a534df65c4b4cae43d0365e52035/475
        String.format(OPENSEA_ASSET_URL, NFT_NETWORK, contract.address, tokenId)
    }
}
