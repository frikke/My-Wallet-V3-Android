package piuk.blockchain.android.ui.maintenance.data.mapper

import com.blockchain.preferences.AppUpdatePrefs
import com.google.android.play.core.appupdate.AppUpdateInfo
import piuk.blockchain.android.data.Mapper
import piuk.blockchain.android.ui.maintenance.data.model.AppMaintenanceConfigDto
import piuk.blockchain.android.ui.maintenance.domain.model.AppMaintenanceConfig

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