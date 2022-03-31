package com.blockchain.blockchaincard.viewmodel

import androidx.navigation.NavHostController
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationDestination
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationRouter

class BlockchainCardNavigationRouter : ComposeNavigationRouter {

    override fun route(navigationEvent: NavigationEvent) {
        var destination: BlockchainCardDestination = BlockchainCardDestination.NoDestination

        when (navigationEvent) {
            is BlockchainCardNavigationEvent.OrderOrLinkCard -> {
                destination = BlockchainCardDestination.OrderOrLinkCardDestination
            }

            is BlockchainCardNavigationEvent.SelectCardForOrder -> {
                destination = BlockchainCardDestination.SelectCardForOrderDestination
            }

            is BlockchainCardNavigationEvent.SeeProductDetails -> {
                destination = BlockchainCardDestination.SeeProductDetailsDestination
            }

            is BlockchainCardNavigationEvent.HideBottomSheet -> {
                navController.popBackStack()
            }

            is BlockchainCardNavigationEvent.CreateCardInProgress -> {
                destination = BlockchainCardDestination.CreateCardInProgressDestination
            }

            is BlockchainCardNavigationEvent.CreateCardSuccess -> {
                navController.popBackStack(BlockchainCardDestination.OrderOrLinkCardDestination.name, true)
                destination = BlockchainCardDestination.CreateCardSuccessDestination
            }

            is BlockchainCardNavigationEvent.CreateCardFailed -> {
                navController.popBackStack(BlockchainCardDestination.SelectCardForOrderDestination.name, false)
                destination = BlockchainCardDestination.CreateCardFailedDestination
            }

            is BlockchainCardNavigationEvent.ManageCard -> {
                navController.popBackStack(BlockchainCardDestination.OrderOrLinkCardDestination.name, true)
                destination = BlockchainCardDestination.ManageCardDestination
            }
        }

        if (destination !is BlockchainCardDestination.NoDestination)
            navController.navigate(destination.name)
    }

    override lateinit var navController: NavHostController
}

sealed class BlockchainCardNavigationEvent : NavigationEvent {
    object OrderOrLinkCard : BlockchainCardNavigationEvent()

    object CreateCardInProgress : BlockchainCardNavigationEvent()

    object CreateCardSuccess : BlockchainCardNavigationEvent()

    object CreateCardFailed : BlockchainCardNavigationEvent()

    object SelectCardForOrder : BlockchainCardNavigationEvent()

    object HideBottomSheet : BlockchainCardNavigationEvent()

    object SeeProductDetails : BlockchainCardNavigationEvent()

    object ManageCard : BlockchainCardNavigationEvent()
}

sealed class BlockchainCardDestination(override val name: String) : ComposeNavigationDestination {

    object NoDestination : BlockchainCardDestination(name = "")

    object OrderOrLinkCardDestination : BlockchainCardDestination(name = "order_or_link_card")

    object CreateCardInProgressDestination : BlockchainCardDestination(name = "create_card_in_progress")

    object CreateCardSuccessDestination : BlockchainCardDestination(name = "create_card_success")

    object CreateCardFailedDestination : BlockchainCardDestination(name = "create_card_failed")

    object SelectCardForOrderDestination : BlockchainCardDestination(name = "select_card_for_order")

    object SeeProductDetailsDestination : BlockchainCardDestination(name = "product_details")

    object ManageCardDestination : BlockchainCardDestination(name = "manage_card")
}
