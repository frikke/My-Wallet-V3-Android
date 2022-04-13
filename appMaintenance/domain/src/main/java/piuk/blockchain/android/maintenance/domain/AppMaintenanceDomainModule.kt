package piuk.blockchain.android.maintenance.domain

import org.koin.dsl.module
import piuk.blockchain.android.maintenance.domain.usecase.GetAppMaintenanceConfigUseCase
import piuk.blockchain.android.maintenance.domain.usecase.IsDownloadInProgressUseCase
import piuk.blockchain.android.maintenance.domain.usecase.SkipAppUpdateUseCase

val appMaintenanceDomainModule = module {
    single { GetAppMaintenanceConfigUseCase(repository = get()) }
    single { IsDownloadInProgressUseCase(repository = get()) }
    single { SkipAppUpdateUseCase(repository = get()) }
}

