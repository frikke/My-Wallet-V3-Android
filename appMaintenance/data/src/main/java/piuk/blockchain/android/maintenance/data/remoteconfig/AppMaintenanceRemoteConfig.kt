package piuk.blockchain.android.maintenance.data.remoteconfig

import com.blockchain.domain.experiments.RemoteConfigService
import com.blockchain.preferences.AppMaintenancePrefs
import kotlinx.coroutines.rx3.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import piuk.blockchain.android.maintenance.data.model.AppMaintenanceConfigDto

internal class AppMaintenanceRemoteConfig(
    private val remoteConfigService: RemoteConfigService,
    private val json: Json,
    private val appMaintenancePrefs: AppMaintenancePrefs
) {
    companion object {
        private const val APP_MAINTENANCE_KEY = "android_app_maintenance"
    }

    suspend fun getAppMaintenanceConfig(): AppMaintenanceConfigDto {
        return getAppMaintenanceJson().let { appMaintenanceConfigJson ->
            if (appMaintenanceConfigJson.isEmpty()) throw Exception("remote config json not found")
            else json.decodeFromString(appMaintenanceConfigJson)
        }
    }

    private suspend fun getAppMaintenanceJson(): String {
        return if (appMaintenancePrefs.isAppMaintenanceDebugOverrideEnabled) {
            appMaintenancePrefs.appMaintenanceDebugJson
        } else {
            remoteConfigService.getRawJson(APP_MAINTENANCE_KEY).await()
        }
    }
}
