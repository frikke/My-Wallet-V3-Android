package com.blockchain.blockchaincard.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed class BlockchainCardNavigationEvent(val name: String) : NavigationEvent {
    object BlockchainCardDestination : BlockchainCardNavigationEvent(name = "blockchain_card")
    object OrderOrLinkCardDestination : BlockchainCardNavigationEvent(name = "order_or_link_card")
}