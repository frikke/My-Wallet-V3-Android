package piuk.blockchain.android.ui.maintenance.data.remoteconfig

import com.blockchain.remoteconfig.RemoteConfig
import kotlinx.coroutines.rx3.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import piuk.blockchain.android.ui.maintenance.data.model.AppMaintenanceConfigDto

internal class AppMaintenanceRemoteConfig(
    private val remoteConfig: RemoteConfig,
    private val json: Json
) {
    companion object {
        const val APP_MAINTENANCE_KEY = "android_app_maintenance"
    }

    suspend fun getAppMaintenanceConfig(): AppMaintenanceConfigDto? {
        return remoteConfig.getRawJson(APP_MAINTENANCE_KEY).await().let { appMaintenanceConfigJson ->
            if (appMaintenanceConfigJson.isEmpty()) null
            else json.decodeFromString(appMaintenanceConfigJson)
        }
    }
}