package com.blockchain.nfts.detail

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.data.updateDataWith
import com.blockchain.nfts.NFT_NETWORK
import com.blockchain.nfts.OPENSEA_ASSET_URL
import com.blockchain.nfts.detail.navigation.NftDetailNavigationEvent
import com.blockchain.nfts.domain.models.NftAsset
import com.blockchain.nfts.domain.service.NftService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NftDetailViewModel(
    val nftId: String,
    val address: String,
    val pageKey: String?,
    private val nftService: NftService
) : MviViewModel<NftDetailIntent,
    NftDetailViewState,
    NftDetailModelState,
    NftDetailNavigationEvent,
    ModelConfigArgs.NoArgs>(
    initialState = NftDetailModelState()
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: NftDetailModelState): NftDetailViewState = state.run {
        NftDetailViewState(
            nftAsset = asset
        )
    }

    override suspend fun handleIntent(modelState: NftDetailModelState, intent: NftDetailIntent) {
        when (intent) {
            NftDetailIntent.LoadData -> {
                loadNftAsset(nftId = nftId, pageKey = pageKey, address = address)
            }

            is NftDetailIntent.ExternalViewRequested -> {
                navigate(
                    NftDetailNavigationEvent.ExternalView(
                        url = intent.nftAsset.getOpenSeaUrl()
                    )
                )
            }
        }
    }

    private fun loadNftAsset(nftId: String, pageKey: String?, address: String) {
        viewModelScope.launch {
            nftService.getNftAsset(
                address = "0x5D70101143BF7bbc889D757613e2B2761bD447EC"/*address*/,
                nftId = nftId,
                pageKey = pageKey
            ).collectLatest { dataResource ->
                updateState {
                    it.copy(
                        asset = it.asset.updateDataWith(dataResource)
                    )
                }

                if (dataResource is DataResource.Error) dataResource.error.printStackTrace()
            }
        }
    }

    private fun NftAsset.getOpenSeaUrl(): String = run {
        // https://opensea.io/assets/ethereum/0x2809a8737477a534df65c4b4cae43d0365e52035/475
        String.format(OPENSEA_ASSET_URL, NFT_NETWORK, contract.address, tokenId)
    }
}
