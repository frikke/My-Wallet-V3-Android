package piuk.blockchain.android.ui.maintenance.presentation

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appMaintenancePresentationModule = module {
    viewModel {
        AppMaintenanceSharedViewModel()
    }

    viewModel {
        AppMaintenanceViewModel(
            getAppMaintenanceConfigUseCase = get(),
            skipAppUpdateUseCase = get()
        )
    }
}

