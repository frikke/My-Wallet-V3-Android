package piuk.blockchain.android.maintenance.domain

import org.koin.dsl.module
import piuk.blockchain.android.maintenance.domain.usecase.GetAppMaintenanceConfigUseCase
import piuk.blockchain.android.maintenance.domain.usecase.IsDownloadInProgressUseCase

val appMaintenanceDomainModule = module {
    single { GetAppMaintenanceConfigUseCase(service = get()) }
    single { IsDownloadInProgressUseCase(service = get()) }
}
