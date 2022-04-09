package piuk.blockchain.android.ui.maintenance.presentation

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface AppMaintenanceIntents : Intent<AppMaintenanceModelState> {
    object ViewStatus : AppMaintenanceIntents
    object RedirectToWebsite : AppMaintenanceIntents
    object SkipUpdate : AppMaintenanceIntents
    object UpdateApp : AppMaintenanceIntents
}