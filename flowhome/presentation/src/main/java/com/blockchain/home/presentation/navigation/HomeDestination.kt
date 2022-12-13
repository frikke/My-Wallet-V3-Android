package com.blockchain.home.presentation.navigation

import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationDestination
import com.blockchain.commonarch.presentation.mvi_v2.compose.wrappedArg

const val ARG_FIAT_TICKER = "fiatTicker"

sealed class HomeDestination(
    override val route: String
) : ComposeNavigationDestination {
    object CryptoAssets : HomeDestination("AllAssets")
    object Activity : HomeDestination("Activity")
    object Referral : HomeDestination("Referral")
    object FiatActionDetail : HomeDestination("FiatActionDetail/${ARG_FIAT_TICKER.wrappedArg()}")
    object MoreQuickActions : HomeDestination("MoreQuickActions")
}
