package piuk.blockchain.android.maintenance.domain.repository

import com.blockchain.outcome.Outcome
import piuk.blockchain.android.maintenance.domain.model.AppMaintenanceConfig

interface AppMaintenanceService {
    suspend fun getAppMaintenanceConfig(): Outcome<Throwable, AppMaintenanceConfig>
    suspend fun isDownloadInProgress(): Boolean
}
