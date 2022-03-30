package com.blockchain.blockchaincard.viewmodel

import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationRouter

class BlockchainCardNavigationRouter : ComposeNavigationRouter {

    override fun route(navigationEvent: ComposeNavigationEvent) {
        if (navigationEvent is BlockchainCardNavigationEvent.CreateCardSuccessDestination
            || navigationEvent is BlockchainCardNavigationEvent.CreateCardFailedDestination) {
            navController.popBackStack(BlockchainCardNavigationEvent.SelectCardForOrder.name, false)
        }

        if (navigationEvent is BlockchainCardNavigationEvent.HideBottomSheet)
            navController.popBackStack()
        else
            navController.navigate(navigationEvent.name)
    }

    override lateinit var navController: NavHostController
}