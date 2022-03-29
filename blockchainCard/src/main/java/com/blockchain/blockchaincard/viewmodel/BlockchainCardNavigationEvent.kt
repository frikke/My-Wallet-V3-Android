package com.blockchain.blockchaincard.viewmodel

import com.blockchain.blockchaincard.data.BlockchainDebitCardProduct
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationEvent

sealed class BlockchainCardNavigationEvent(name: String) : ComposeNavigationEvent(name) {
    object BlockchainCardDestination : BlockchainCardNavigationEvent(name = "blockchain_card")

    object OrderOrLinkCardDestination : BlockchainCardNavigationEvent(name = "order_or_link_card")

    object CreateCardInProgressDestination : BlockchainCardNavigationEvent(name = "create_card_in_progress")

    object CreateCardSuccessDestination : BlockchainCardNavigationEvent(name = "create_card_success")

    object CreateCardFailedDestination : BlockchainCardNavigationEvent(name = "create_card_failed")

    object SelectCardForOrder : BlockchainCardNavigationEvent(name = "select_card_for_order")

    data class OnSeeProductDetails(val cardProduct: BlockchainDebitCardProduct)
        : BlockchainCardNavigationEvent(name = "product_details")
}