package com.blockchain.presentation.onboarding.navigation

import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationDestination

sealed class DeFiOnboardingDestination(override val route: String) : ComposeNavigationDestination {
    object DeFiOnboardingIntro : DeFiOnboardingDestination("DeFiOnboardingIntro")
    object DeFiOnboardingComplete : DeFiOnboardingDestination("DeFiOnboardingComplete")
}
