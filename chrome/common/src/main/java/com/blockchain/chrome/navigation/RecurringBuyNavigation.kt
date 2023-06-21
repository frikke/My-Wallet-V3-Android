package com.blockchain.chrome.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

@Stable
interface RecurringBuyNavigation {
    fun openOnboarding()
}

val LocalRecurringBuyNavigationProvider = staticCompositionLocalOf<RecurringBuyNavigation> {
    error("No AssetActionsNavigation provided.")
}
