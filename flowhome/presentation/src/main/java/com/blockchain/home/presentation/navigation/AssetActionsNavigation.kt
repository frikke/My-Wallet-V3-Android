package com.blockchain.home.presentation.navigation

import com.blockchain.coincore.AssetAction
import com.blockchain.domain.onboarding.CompletableDashboardOnboardingStep
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money

interface AssetActionsNavigation {
    fun navigate(assetAction: AssetAction)
    fun buyCrypto(currency: AssetInfo, amount: Money?)
    fun earnRewards()
    fun onBoardingNavigation(initialSteps: List<CompletableDashboardOnboardingStep>)
}
