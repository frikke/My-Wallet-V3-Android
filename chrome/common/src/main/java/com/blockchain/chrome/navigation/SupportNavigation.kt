package com.blockchain.chrome.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

@Stable
interface SupportNavigation {
    fun launchSupportCenter()
    fun launchSupportChat()
}

val LocalSupportNavigationProvider = staticCompositionLocalOf<SupportNavigation> {
    error("not provided")
}
