package com.blockchain.presentation.onboarding.navigation

import androidx.navigation.NavHostController
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationRouter
import com.blockchain.extensions.exhaustive

class DeFiOnboardingNavigationRouter(
    override val navController: NavHostController
) : ComposeNavigationRouter<DeFiOnboardingNavigationEvent> {

    override fun route(navigationEvent: DeFiOnboardingNavigationEvent) {
        when (navigationEvent) {
            DeFiOnboardingNavigationEvent.DeFiOnboardingIntro -> {
                navController.navigate(DeFiOnboardingDestination.DeFiOnboardingIntro.route)
            }
        }.exhaustive
    }
}
