package com.blockchain.nfts.detail

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.nfts.domain.models.NftAsset

data class NftDetailViewState(
    val nftAsset: DataResource<NftAsset?>
) : ViewState
