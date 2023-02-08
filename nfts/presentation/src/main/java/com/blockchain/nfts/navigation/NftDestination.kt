package com.blockchain.nfts.navigation

import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationDestination

sealed class NftDestination(
    override val route: String
) : ComposeNavigationDestination {
    object Help : NftDestination("NftHelp")
}
