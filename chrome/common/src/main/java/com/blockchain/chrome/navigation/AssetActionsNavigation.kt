package com.blockchain.chrome.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.CustodialInterestAccount
import com.blockchain.domain.onboarding.CompletableDashboardOnboardingStep
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.navigation.ActivityResultNavigation
import info.blockchain.balance.AssetInfo

@Stable
interface AssetActionsNavigation : ActivityResultNavigation {
    fun navigate(assetAction: AssetAction)
    fun receive(currency: String)
    fun receive(account: CryptoNonCustodialAccount)
    fun buyCrypto(
        currency: AssetInfo,
        amount: String? = null,
        preselectedFiatTicker: String? = null,
        launchLinkCard: Boolean = false,
        launchNewPaymentMethodSelection: Boolean = false
    )
    fun buyCryptoWithRecurringBuy()

    fun buyWithPreselectedMethod(paymentMethodId: String?)
    fun settings()
    fun fundsLocksDetail(fundsLocks: FundsLocks)
    fun coinview(asset: AssetInfo)
    fun coinview(asset: AssetInfo, recurringBuyId: String?, originScreen: String)
    fun onBoardingNavigation(initialSteps: List<CompletableDashboardOnboardingStep>)
    fun interestSummary(account: CryptoAccount)
    fun interestDeposit(source: CryptoAccount, target: CustodialInterestAccount)
    fun stakingSummary(networkTicker: String)
    fun activeRewardsSummary(networkTicker: String)
    fun startKyc()

    fun initNewTxFlowFFs()
}

val LocalAssetActionsNavigationProvider = staticCompositionLocalOf<AssetActionsNavigation> {
    error("No AssetActionsNavigation provided.")
}

