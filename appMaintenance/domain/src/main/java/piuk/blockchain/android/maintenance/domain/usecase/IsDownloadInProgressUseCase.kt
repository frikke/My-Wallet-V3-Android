package piuk.blockchain.android.maintenance.domain.usecase

import piuk.blockchain.android.maintenance.domain.repository.AppMaintenanceService

class IsDownloadInProgressUseCase(private val service: AppMaintenanceService) {
    suspend operator fun invoke(): Boolean = service.isDownloadInProgress()
}
