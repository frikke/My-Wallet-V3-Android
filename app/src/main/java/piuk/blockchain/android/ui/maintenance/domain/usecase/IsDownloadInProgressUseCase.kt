package piuk.blockchain.android.ui.maintenance.domain.usecase

import piuk.blockchain.android.ui.maintenance.domain.repository.AppMaintenanceRepository

class IsDownloadInProgressUseCase(private val repository: AppMaintenanceRepository) {
    suspend operator fun invoke(): Boolean = repository.isDownloadInProgress()
}