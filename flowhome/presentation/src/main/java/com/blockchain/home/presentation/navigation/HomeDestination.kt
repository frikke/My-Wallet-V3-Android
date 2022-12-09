package com.blockchain.home.presentation.navigation

import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationDestination

sealed class HomeDestination(
    override val route: String
) : ComposeNavigationDestination {
    object CryptoAssets : HomeDestination("AllAssets")
    object Activity : HomeDestination("Activity")
    object Referral : HomeDestination("Referral")
    object FiatActionDetail : HomeDestination("FiatActionDetail")
}
