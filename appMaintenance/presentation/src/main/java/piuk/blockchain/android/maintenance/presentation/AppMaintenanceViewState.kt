package piuk.blockchain.android.maintenance.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ViewState

data class AppMaintenanceViewState(
    val uiState: AppMaintenanceStatusUiState
) : ViewState
