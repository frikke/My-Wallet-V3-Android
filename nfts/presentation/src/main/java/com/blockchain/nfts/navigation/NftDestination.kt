package com.blockchain.nfts.navigation

import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationDestination
import com.blockchain.commonarch.presentation.mvi_v2.compose.wrappedArg

const val ARG_NFT_ID = "nftId"
const val ARG_ADDRESS = "address"
const val ARG_PAGE_KEY = "pageKey"

sealed class NftDestination(
    override val route: String
) : ComposeNavigationDestination {
    object Help : NftDestination("NftHelp")

    object Detail : NftDestination(
        route = "NftDetail/${ARG_NFT_ID.wrappedArg()}/${ARG_ADDRESS.wrappedArg()}/${ARG_PAGE_KEY.wrappedArg()}"
    )

    object ReceiveAccountDetail : NftDestination("ReceiveAccountDetail")
}
