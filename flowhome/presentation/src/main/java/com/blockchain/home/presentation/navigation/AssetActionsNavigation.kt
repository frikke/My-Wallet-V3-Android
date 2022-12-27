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
    fun receive(currency: String)
    fun buyCrypto(currency: AssetInfo, amount: Money)
    fun buyCrypto(
        currency: AssetInfo,
        amount: String? = null,
        preselectedFiatTicker: String? = null,
        launchLinkCard: Boolean = false,
        launchNewPaymentMethodSelection: Boolean = false,
    )
    fun buyWithPreselectedMethod(paymentMethodId: String?)

    fun earnRewards()
    fun settings()
    fun coinview(asset: AssetInfo)
    fun coinview(asset: AssetInfo, recurringBuyId: String?, originScreen: String)
    fun onBoardingNavigation(initialSteps: List<CompletableDashboardOnboardingStep>)
    fun interestSummary(account: CryptoAccount)
    fun stakingSummary(currency: Currency)
    fun startKyc()
}
