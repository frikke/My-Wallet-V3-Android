package piuk.blockchain.android.ui.maintenance.domain

import org.koin.dsl.module
import piuk.blockchain.android.ui.maintenance.domain.usecase.GetAppMaintenanceConfigUseCase
import piuk.blockchain.android.ui.maintenance.domain.usecase.SkipAppUpdateUseCase

val appMaintenanceDomainModule = module {
    single { GetAppMaintenanceConfigUseCase(repository = get()) }
    single { SkipAppUpdateUseCase(repository = get()) }
}

