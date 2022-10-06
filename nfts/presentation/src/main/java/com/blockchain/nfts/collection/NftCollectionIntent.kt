package com.blockchain.nfts.collection

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface NftCollectionIntent : Intent<NftCollectionModelState> {
    data class LoadData(val isFromPullToRefresh: Boolean = false) : NftCollectionIntent
    object ExternalShop : NftCollectionIntent
    object ShowReceiveAddress : NftCollectionIntent
    object ShowHelp : NftCollectionIntent
    data class ShowDetail(val nftId: String) : NftCollectionIntent
}
