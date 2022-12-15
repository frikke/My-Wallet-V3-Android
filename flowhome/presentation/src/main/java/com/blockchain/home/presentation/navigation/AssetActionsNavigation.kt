package com.blockchain.home.presentation.navigation

import androidx.compose.runtime.Stable
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.domain.onboarding.CompletableDashboardOnboardingStep
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.Money

@Stable
interface AssetActionsNavigation {
    fun navigate(assetAction: AssetAction)
    fun buyCrypto(currency: AssetInfo, amount: Money?)
    fun earnRewards()
    fun settings()
    fun coinview(asset: AssetInfo)
    fun onBoardingNavigation(initialSteps: List<CompletableDashboardOnboardingStep>)
    fun interestSummary(account: CryptoAccount)
    fun stakingSummary(currency: Currency)
}
