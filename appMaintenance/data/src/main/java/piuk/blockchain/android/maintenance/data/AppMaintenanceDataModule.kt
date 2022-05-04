package piuk.blockchain.android.maintenance.data

import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module
import piuk.blockchain.android.maintenance.data.appupdateapi.AppUpdateInfoFactoryImpl
import piuk.blockchain.android.maintenance.data.remoteconfig.AppMaintenanceRemoteConfig
import piuk.blockchain.android.maintenance.data.repository.AppMaintenanceRepository
import piuk.blockchain.android.maintenance.domain.appupdateapi.AppUpdateInfoFactory
import piuk.blockchain.android.maintenance.domain.repository.AppMaintenanceService

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

    single<AppMaintenanceService> {
        AppMaintenanceRepository(
            appMaintenanceRemoteConfig = get(),
            appUpdateInfoFactory = get(),
            versionsInfo = get(),
            dispatcher = Dispatchers.IO
        )
    }
}
