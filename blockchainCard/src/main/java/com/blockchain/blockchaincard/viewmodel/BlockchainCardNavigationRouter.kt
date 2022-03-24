package com.blockchain.blockchaincard.viewmodel

import androidx.navigation.NavHostController
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationRouter

class BlockchainCardNavigationRouter : ComposeNavigationRouter {

    override fun route(navigationEvent: ComposeNavigationEvent) {
        var route = navigationEvent.name
        if (navigationEvent is BlockchainCardNavigationEvent.OnSeeProductDetails) {
            route = "$route?" +
                "brand=${navigationEvent.cardProduct.brand}&" +
                "type=${navigationEvent.cardProduct.type}&" +
                "price=${navigationEvent.cardProduct.price.toStringWithSymbol()}"
        }
        navController.navigate(route)
    }

    override lateinit var navController: NavHostController
}