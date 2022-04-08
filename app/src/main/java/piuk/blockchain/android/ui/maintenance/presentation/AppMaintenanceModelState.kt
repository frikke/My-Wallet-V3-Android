package piuk.blockchain.android.ui.maintenance.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ModelState

data class AppMaintenanceModelState(
    val appMaintenanceStatusUi: AppMaintenanceStatusUi = AppMaintenanceStatusUi.NO_STATUS
) : ModelState