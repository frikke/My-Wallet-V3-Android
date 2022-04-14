package piuk.blockchain.android.maintenance.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import piuk.blockchain.android.maintenance.domain.model.AppMaintenanceStatus

data class AppMaintenanceModelState(
    val status: AppMaintenanceStatus.Actionable? = null
) : ModelState