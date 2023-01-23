package com.blockchain.home.presentation.navigation

import androidx.compose.runtime.Stable

@Stable
interface SettingsNavigation {
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
