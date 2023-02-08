package com.blockchain.nfts.detail

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.nfts.domain.models.NftAsset

sealed interface NftDetailIntent : Intent<NftDetailModelState> {
    object LoadData : NftDetailIntent
    data class ExternalViewRequested(val nftAsset: NftAsset) : NftDetailIntent
}
