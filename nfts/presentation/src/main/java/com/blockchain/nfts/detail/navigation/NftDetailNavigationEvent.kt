package com.blockchain.nfts.detail.navigation

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed interface NftDetailNavigationEvent : NavigationEvent {
    /**
     * Currently opens in OpenSea
     */
    data class ExternalView(val url: String) : NftDetailNavigationEvent
}
