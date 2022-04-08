package piuk.blockchain.android.ui.maintenance.domain

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import piuk.blockchain.android.ui.maintenance.presentation.AppMaintenanceViewModel

val interestDashboardDomainModule = module {
    viewModel {
        AppMaintenanceViewModel(
            getAppMaintenanceConfigUseCase = get()
        )
    }
}

