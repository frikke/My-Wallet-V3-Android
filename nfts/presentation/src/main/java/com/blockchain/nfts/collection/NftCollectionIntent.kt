package com.blockchain.nfts.collection

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface NftCollectionIntent : Intent<NftCollectionModelState> {
    object ExternalShop : NftCollectionIntent
}
