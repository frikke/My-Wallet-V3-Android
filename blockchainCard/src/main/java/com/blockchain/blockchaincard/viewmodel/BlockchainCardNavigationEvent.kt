package com.blockchain.blockchaincard.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationEvent

sealed class BlockchainCardNavigationEvent(override val name: String) : ComposeNavigationEvent {
    object OrderOrLinkCardDestination : BlockchainCardNavigationEvent(name = "order_or_link_card")

    object CreateCardInProgressDestination : BlockchainCardNavigationEvent(name = "create_card_in_progress")

    object CreateCardSuccessDestination : BlockchainCardNavigationEvent(name = "create_card_success")

    object CreateCardFailedDestination : BlockchainCardNavigationEvent(name = "create_card_failed")

    object SelectCardForOrder : BlockchainCardNavigationEvent(name = "select_card_for_order")

    object HideBottomSheet : BlockchainCardNavigationEvent(name = "hide_bottom_sheet")

    object OnSeeProductDetails : BlockchainCardNavigationEvent(name = "product_details")

    object ManageCardDestination : BlockchainCardNavigationEvent(name = "manage_card")
}
