package piuk.blockchain.android.ui.maintenance.presentation

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed interface AppMaintenanceNavigationEvent : NavigationEvent {
    object Close : AppMaintenanceNavigationEvent
}