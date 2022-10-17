package com.blockchain.home.presentation.navigation

import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationDestination

sealed class HomeDestination(override val route: String) : ComposeNavigationDestination {
    object CryptoAssets : HomeDestination("AllAssets")
}
