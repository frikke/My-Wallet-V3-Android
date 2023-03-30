package com.blockchain.home.presentation.navigation

import androidx.compose.runtime.Stable
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.domain.onboarding.CompletableDashboardOnboardingStep
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.navigation.ActivityResultNavigation
import info.blockchain.balance.AssetInfo

@Stable
interface RecurringBuyNavigation {
    fun openOnboarding()
}
