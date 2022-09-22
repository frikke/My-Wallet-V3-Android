package com.blockchain.nfts.collection

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.nfts.domain.models.NftAsset

data class NftCollectionViewState(
    val collection: DataResource<List<NftAsset>>
) : ViewState
