package piuk.blockchain.android.ui.maintenance.domain.repository

import com.blockchain.outcome.Outcome
import piuk.blockchain.android.ui.maintenance.domain.model.AppMaintenanceConfig

interface AppMaintenanceRepository {
    suspend fun getAppMaintenanceConfig(): Outcome<Throwable, AppMaintenanceConfig>
}
