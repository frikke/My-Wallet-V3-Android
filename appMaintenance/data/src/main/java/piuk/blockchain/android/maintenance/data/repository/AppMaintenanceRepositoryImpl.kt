package piuk.blockchain.android.maintenance.data.repository

import com.blockchain.outcome.Outcome
import com.google.android.play.core.appupdate.AppUpdateInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import piuk.blockchain.android.maintenance.data.appupdateapi.isDownloadTriggered
import piuk.blockchain.android.maintenance.data.mapper.AppMaintenanceConfigMapper
import piuk.blockchain.android.maintenance.data.model.AppMaintenanceConfigDto
import piuk.blockchain.android.maintenance.data.remoteconfig.AppMaintenanceRemoteConfig
import piuk.blockchain.android.maintenance.domain.appupdateapi.AppUpdateInfoFactory
import piuk.blockchain.android.maintenance.domain.model.AppMaintenanceConfig
import piuk.blockchain.android.maintenance.domain.repository.AppMaintenanceRepository
import timber.log.Timber

internal class AppMaintenanceRepositoryImpl(
    private val appMaintenanceRemoteConfig: AppMaintenanceRemoteConfig,
    private val appUpdateInfoFactory: AppUpdateInfoFactory,
    private val currentVersionCode: Int,
    private val currentOsVersion: Int,
    private val dispatcher: CoroutineDispatcher
) : AppMaintenanceRepository {

    override suspend fun getAppMaintenanceConfig(): Outcome<Throwable, AppMaintenanceConfig> {
        return supervisorScope {
            val deferredMaintenanceConfig = async(dispatcher) { appMaintenanceRemoteConfig.getAppMaintenanceConfig() }
            val deferredAppUpdateInfo = async(dispatcher) { appUpdateInfoFactory.getAppUpdateInfo() }

            val maintenanceConfig: AppMaintenanceConfigDto? = try {
                deferredMaintenanceConfig.await()
            } catch (e: Throwable) {
                Timber.e(e)
                null
            }

            val appUpdateInfo: AppUpdateInfo? = try {
                deferredAppUpdateInfo.await()
            } catch (e: Throwable) {
                Timber.e("Cannot get appUpdateInfo, $e")
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
                    AppMaintenanceConfigMapper.map(
                        appUpdateInfo, maintenanceConfig, currentVersionCode, currentOsVersion
                    )
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
}