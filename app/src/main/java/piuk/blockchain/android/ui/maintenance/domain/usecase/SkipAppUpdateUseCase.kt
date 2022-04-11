package piuk.blockchain.android.ui.maintenance.domain.usecase

import piuk.blockchain.android.ui.maintenance.domain.repository.AppMaintenanceRepository

class SkipAppUpdateUseCase(private val repository: AppMaintenanceRepository) {
    operator fun invoke(versionCode: Int) = repository.skipAppUpdate(versionCode)
}
