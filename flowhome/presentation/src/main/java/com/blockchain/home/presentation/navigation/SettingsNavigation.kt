package com.blockchain.home.presentation.navigation

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
