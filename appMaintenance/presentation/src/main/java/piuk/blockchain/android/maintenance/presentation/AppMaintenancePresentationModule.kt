package piuk.blockchain.android.maintenance.presentation

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import piuk.blockchain.android.maintenance.presentation.appupdateapi.InAppUpdateSettings
import piuk.blockchain.android.maintenance.presentation.appupdateapi.InAppUpdateSettingsImpl

val appMaintenancePresentationModule = module {
    viewModel {
        AppMaintenanceSharedViewModel()
    }

    viewModel {
        AppMaintenanceViewModel(
            getAppMaintenanceConfigUseCase = get(),
            isDownloadInProgressUseCase = get()
        )
    }

    single<InAppUpdateSettings> {
        InAppUpdateSettingsImpl(
            appUpdateManager = get(),
            appUpdateInfoFactory = get()
        )
    }
}
