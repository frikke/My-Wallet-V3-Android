package piuk.blockchain.android.ui.maintenance.domain

import org.koin.dsl.module
import piuk.blockchain.android.ui.maintenance.domain.usecase.GetAppMaintenanceConfigUseCase

val interestDashboardDomainModule = module {
    single { GetAppMaintenanceConfigUseCase(repository = get()) }
}

