package com.blockchain.nfts.collection

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface NftCollectionIntent : Intent<NftCollectionModelState> {
    data class LoadData(val isFromPullToRefresh: Boolean = false) : NftCollectionIntent

    data class ChangeDisplayType(val displayType: DisplayType) : NftCollectionIntent {
        override fun isValidFor(modelState: NftCollectionModelState): Boolean {
            return modelState.displayType != displayType
        }
    }

    object LoadNextPage : NftCollectionIntent {
        override fun isValidFor(modelState: NftCollectionModelState): Boolean {
            return modelState.nextPageKey.isNullOrBlank().not()
        }
    }

    object ExternalShop : NftCollectionIntent
    object ShowReceiveAddress : NftCollectionIntent
    object ShowHelp : NftCollectionIntent
    data class ShowDetail(val nftId: String, val pageKey: String?) : NftCollectionIntent
}
