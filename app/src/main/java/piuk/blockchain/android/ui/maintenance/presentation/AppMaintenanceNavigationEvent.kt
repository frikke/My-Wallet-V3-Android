package piuk.blockchain.android.ui.maintenance.presentation

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed interface AppMaintenanceNavigationEvent : NavigationEvent {
    data class OpenUrl(val url: String) : AppMaintenanceNavigationEvent
    object LaunchAppUpdate : AppMaintenanceNavigationEvent
    object ResumeAppFlow : AppMaintenanceNavigationEvent
}