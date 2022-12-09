package com.blockchain.home.presentation.navigation

import androidx.compose.runtime.Stable
import com.blockchain.chrome.navigation.AppNavigation
import com.blockchain.coincore.AssetAction
import com.blockchain.domain.onboarding.CompletableDashboardOnboardingStep
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
@Stable
interface AssetActionsNavigation : AppNavigation {
    fun navigate(assetAction: AssetAction)
    fun buyCrypto(currency: AssetInfo, amount: Money?)
    fun earnRewards()
    fun coinview(asset: AssetInfo)
    fun onBoardingNavigation(initialSteps: List<CompletableDashboardOnboardingStep>)
}
