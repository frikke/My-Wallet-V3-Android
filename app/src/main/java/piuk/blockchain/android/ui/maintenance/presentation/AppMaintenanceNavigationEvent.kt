package piuk.blockchain.android.ui.maintenance.presentation

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed interface AppMaintenanceNavigationEvent : NavigationEvent {
    data class RedirectToWebsite(val websiteUrl: String) : AppMaintenanceNavigationEvent
    data class ViewStatus(val statusUrl: String) : AppMaintenanceNavigationEvent
    object Close : AppMaintenanceNavigationEvent
}