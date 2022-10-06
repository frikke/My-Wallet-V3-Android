package com.blockchain.nfts.detail

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.nfts.domain.models.NftAsset

data class NftDetailModelState(
    val asset: DataResource<NftAsset?> = DataResource.Loading
) : ModelState
