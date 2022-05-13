package com.blockchain.blockchaincard.viewmodel

import androidx.navigation.NavHostController
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationDestination
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationRouter
import com.blockchain.extensions.exhaustive

class BlockchainCardNavigationRouter(override val navController: NavHostController) :
    ComposeNavigationRouter<BlockchainCardNavigationEvent> {

    override fun route(navigationEvent: BlockchainCardNavigationEvent) {
        var destination: BlockchainCardDestination = BlockchainCardDestination.NoDestination

        @Suppress("IMPLICIT_CAST_TO_ANY")
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
                navController.popBackStack(BlockchainCardDestination.OrderOrLinkCardDestination.route, true)
                destination = BlockchainCardDestination.CreateCardSuccessDestination
            }

            is BlockchainCardNavigationEvent.CreateCardFailed -> {
                navController.popBackStack(BlockchainCardDestination.SelectCardForOrderDestination.route, false)
                destination = BlockchainCardDestination.CreateCardFailedDestination
            }

            is BlockchainCardNavigationEvent.ManageCard -> {
                navController.popBackStack(BlockchainCardDestination.CreateCardSuccessDestination.route, true)
                destination = BlockchainCardDestination.ManageCardDestination
            }

            is BlockchainCardNavigationEvent.ManageCardDetails -> {
                destination = BlockchainCardDestination.ManageCardDetailsDestination
            }
        }.exhaustive

        if (destination !is BlockchainCardDestination.NoDestination)
            navController.navigate(destination.route)
    }

    /*
        override lateinit var navController: NavHostController
    */
}

sealed class BlockchainCardNavigationEvent : NavigationEvent {

    // Order Card
    object OrderOrLinkCard : BlockchainCardNavigationEvent()

    object CreateCardInProgress : BlockchainCardNavigationEvent()

    object CreateCardSuccess : BlockchainCardNavigationEvent()

    object CreateCardFailed : BlockchainCardNavigationEvent()

    object SelectCardForOrder : BlockchainCardNavigationEvent()

    object HideBottomSheet : BlockchainCardNavigationEvent()

    object SeeProductDetails : BlockchainCardNavigationEvent()

    object ManageCard : BlockchainCardNavigationEvent()

    // Manage Card
    object ManageCardDetails : BlockchainCardNavigationEvent()
}

sealed class BlockchainCardDestination(override val route: String) : ComposeNavigationDestination {

    object NoDestination : BlockchainCardDestination(route = "")

    object OrderOrLinkCardDestination : BlockchainCardDestination(route = "order_or_link_card")

    object CreateCardInProgressDestination : BlockchainCardDestination(route = "create_card_in_progress")

    object CreateCardSuccessDestination : BlockchainCardDestination(route = "create_card_success")

    object CreateCardFailedDestination : BlockchainCardDestination(route = "create_card_failed")

    object SelectCardForOrderDestination : BlockchainCardDestination(route = "select_card_for_order")

    object SeeProductDetailsDestination : BlockchainCardDestination(route = "product_details")

    object ManageCardDestination : BlockchainCardDestination(route = "manage_card")

    object ManageCardDetailsDestination : BlockchainCardDestination(route = "manage_card_details")
}
