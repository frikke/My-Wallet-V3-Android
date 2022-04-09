package piuk.blockchain.android.ui.maintenance.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ViewState

data class AppMaintenanceViewState(
    val statusUiSettings: AppMaintenanceStatusUiSettings
) : ViewState