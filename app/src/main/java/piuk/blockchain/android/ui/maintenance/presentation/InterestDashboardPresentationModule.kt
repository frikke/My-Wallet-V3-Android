package piuk.blockchain.android.ui.maintenance.presentation

import org.koin.dsl.module
import piuk.blockchain.android.ui.maintenance.domain.usecase.GetAppMaintenanceConfigUseCase

val interestDashboardPresentationModule = module {
    single { GetAppMaintenanceConfigUseCase(repository = get()) }
}

