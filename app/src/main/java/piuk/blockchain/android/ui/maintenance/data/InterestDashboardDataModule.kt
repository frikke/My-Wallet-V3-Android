package piuk.blockchain.android.ui.maintenance.data

import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module
import piuk.blockchain.android.ui.maintenance.data.appupdateapi.AppUpdateInfoFactory
import piuk.blockchain.android.ui.maintenance.data.appupdateapi.AppUpdateInfoFactoryImpl
import piuk.blockchain.android.ui.maintenance.data.remoteconfig.AppMaintenanceRemoteConfig
import piuk.blockchain.android.ui.maintenance.data.repository.AppMaintenanceRepositoryImpl
import piuk.blockchain.android.ui.maintenance.domain.repository.AppMaintenanceRepository

val appMaintenanceDataModule = module {
    single {
        AppMaintenanceRemoteConfig(
            remoteConfig = get(),
            json = get()
        )
    }

    single<AppUpdateManager> {
        AppUpdateManagerFactory.create(get())
    }

    single<AppUpdateInfoFactory> {
        AppUpdateInfoFactoryImpl(get())
    }

    single<AppMaintenanceRepository> {
        AppMaintenanceRepositoryImpl(
            appMaintenanceRemoteConfig = get(),
            appUpdateInfoFactory = get(),
            appUpdatePrefs = get(),
            dispatcher = Dispatchers.IO
        )
    }
}

