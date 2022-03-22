package com.blockchain.blockchaincard.viewmodel

import androidx.navigation.NavHostController
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter

class BlockchainCardNavigationRouter : NavigationRouter<BlockchainCardNavigationEvent> {

    public lateinit var navController: NavHostController

    override fun route(navigationEvent: BlockchainCardNavigationEvent) {
        navController.navigate(navigationEvent.name)
    }
}