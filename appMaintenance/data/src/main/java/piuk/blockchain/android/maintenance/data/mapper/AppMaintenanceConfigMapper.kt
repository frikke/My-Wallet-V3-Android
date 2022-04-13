package piuk.blockchain.android.maintenance.data.mapper

import com.blockchain.mapper.Mapper
import com.blockchain.preferences.AppUpdatePrefs
import com.google.android.play.core.appupdate.AppUpdateInfo
import piuk.blockchain.android.maintenance.domain.model.AppMaintenanceConfig
import piuk.blockchain.android.maintenance.data.model.AppMaintenanceConfigDto

internal object AppMaintenanceConfigMapper :
    Mapper<Triple<AppUpdateInfo, AppMaintenanceConfigDto, AppUpdatePrefs>, AppMaintenanceConfig> {

    override fun map(type: Triple<AppUpdateInfo, AppMaintenanceConfigDto, AppUpdatePrefs>): AppMaintenanceConfig {
        return type.let { (appUpdateInfo, maintenanceConfig, appUpdatePrefs) ->
            AppMaintenanceConfig(
                playStoreVersion = appUpdateInfo.availableVersionCode(),
                bannedVersions = maintenanceConfig.bannedVersions,
                minimumAppVersion = maintenanceConfig.minimumAppVersion,
                softUpgradeVersion = maintenanceConfig.softUpgradeVersion,
                skippedSoftVersion = appUpdatePrefs.skippedVersionCode,
                minimumOSVersion = maintenanceConfig.minimumOSVersion,
                siteWideMaintenance = maintenanceConfig.siteWideMaintenance,
                redirectToWebsite = maintenanceConfig.redirectToWebsite,
                statusUrl = maintenanceConfig.statusURL,
                storeUrl = maintenanceConfig.storeURI,
                websiteUrl = maintenanceConfig.websiteUrl
            )
        }
    }
}