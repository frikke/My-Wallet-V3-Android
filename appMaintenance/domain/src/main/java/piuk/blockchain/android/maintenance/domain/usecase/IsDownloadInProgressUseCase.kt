package piuk.blockchain.android.maintenance.domain.usecase

import piuk.blockchain.android.maintenance.domain.repository.AppMaintenanceRepository

class IsDownloadInProgressUseCase(private val repository: AppMaintenanceRepository) {
    suspend operator fun invoke(): Boolean = repository.isDownloadInProgress()
}