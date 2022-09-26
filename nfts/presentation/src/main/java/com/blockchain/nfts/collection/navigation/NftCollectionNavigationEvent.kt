package com.blockchain.nfts.collection.navigation

import com.blockchain.coincore.BlockchainAccount
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed interface NftCollectionNavigationEvent : NavigationEvent {
    /**
     * Currently opens in OpenSea
     */
    data class ShopExternal(val url: String) : NftCollectionNavigationEvent

    data class ShowReceiveAddress(val account: BlockchainAccount) : NftCollectionNavigationEvent

    data class ShowDetail(val nftId: String, val address: String) : NftCollectionNavigationEvent
}
