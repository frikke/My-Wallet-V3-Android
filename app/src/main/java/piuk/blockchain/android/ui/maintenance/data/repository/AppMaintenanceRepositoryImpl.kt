package piuk.blockchain.android.ui.maintenance.data.repository

import com.blockchain.outcome.Outcome
import com.blockchain.preferences.AppUpdatePrefs
import com.google.android.play.core.appupdate.AppUpdateInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import piuk.blockchain.android.ui.maintenance.data.appupdateapi.AppUpdateInfoFactory
import piuk.blockchain.android.ui.maintenance.data.mapper.AppMaintenanceConfigMapper
import piuk.blockchain.android.ui.maintenance.data.model.AppMaintenanceConfigDto
import piuk.blockchain.android.ui.maintenance.data.remoteconfig.AppMaintenanceRemoteConfig
import piuk.blockchain.android.ui.maintenance.domain.appupdateapi.isDownloadTriggered
import piuk.blockchain.android.ui.maintenance.domain.model.AppMaintenanceConfig
import piuk.blockchain.android.ui.maintenance.domain.repository.AppMaintenanceRepository
import timber.log.Timber

internal class AppMaintenanceRepositoryImpl(
    private val appMaintenanceRemoteConfig: AppMaintenanceRemoteConfig,
    private val appUpdateInfoFactory: AppUpdateInfoFactory,
    private val appUpdatePrefs: AppUpdatePrefs,
    private val dispatcher: CoroutineDispatcher
) : AppMaintenanceRepository {

    override suspend fun getAppMaintenanceConfig(): Outcome<Throwable, AppMaintenanceConfig> {
        return supervisorScope {
            val deferredMaintenanceConfig = async(dispatcher) { appMaintenanceRemoteConfig.getAppMaintenanceConfig() }
            val deferredAppUpdateInfo = async { appUpdateInfoFactory.getAppUpdateInfo() }

            val maintenanceConfig: AppMaintenanceConfigDto? = deferredMaintenanceConfig.await()
            val appUpdateInfo: AppUpdateInfo? = try {
                deferredAppUpdateInfo.await()
            } catch (e: Throwable) {
                Timber.e("Cannot get appUpdateInfo, $e")
                e.printStackTrace()
                null
            }

            if (maintenanceConfig == null || appUpdateInfo == null) {
                Outcome.Failure(
                    Throwable(
                        "maintenanceConfig: $maintenanceConfig" +
                            "\nappUpdateInfo: $appUpdateInfo" +
                            "\nboth must not be null"
                    )
                )
            } else {
                Outcome.Success(
                    AppMaintenanceConfigMapper.map(Triple(appUpdateInfo, maintenanceConfig, appUpdatePrefs))
                )
            }
        }
    }

    override suspend fun isDownloadInProgress(): Boolean {
        return supervisorScope {
            try {
                appUpdateInfoFactory.getAppUpdateInfo().isDownloadTriggered()
            } catch (e: Throwable) {
                false
            }
        }
    }

    override fun skipAppUpdate(versionCode: Int) {
        appUpdatePrefs.skippedVersionCode = versionCode
    }
}