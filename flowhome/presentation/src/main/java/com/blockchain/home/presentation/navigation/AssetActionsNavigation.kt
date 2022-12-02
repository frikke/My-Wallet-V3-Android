package com.blockchain.home.presentation.navigation

import com.blockchain.coincore.AssetAction
import com.blockchain.domain.onboarding.CompletableDashboardOnboardingStep

interface AssetActionsNavigation {
    fun navigate(assetAction: AssetAction)
    fun onBoardingNavigation(initialSteps: List<CompletableDashboardOnboardingStep>)
}
