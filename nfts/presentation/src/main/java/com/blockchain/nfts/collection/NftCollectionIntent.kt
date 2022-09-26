package com.blockchain.nfts.collection

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface NftCollectionIntent : Intent<NftCollectionModelState> {
    object LoadData : NftCollectionIntent
    object ExternalShop : NftCollectionIntent
    object ShowReceiveAddress : NftCollectionIntent
    object ShowHelp : NftCollectionIntent
    data class ShowDetail(val nftId: String) : NftCollectionIntent
}
