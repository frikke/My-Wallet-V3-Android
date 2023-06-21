package com.blockchain.chrome.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import com.blockchain.navigation.ActivityResultNavigation

@Stable
interface SettingsNavigation : ActivityResultNavigation {
    fun settings()
    fun settings(settingsDestination: SettingsDestination = SettingsDestination.Home)
    fun launchSupportCenter()
}

enum class SettingsDestination {
    Home,
    Account,
    Notifications,
    Security,
    General,
    About,
    CardLinking,
    BankLinking
}

val LocalSettingsNavigationProvider = staticCompositionLocalOf<SettingsNavigation> {
    error("No AssetActionsNavigation provided.")
}

