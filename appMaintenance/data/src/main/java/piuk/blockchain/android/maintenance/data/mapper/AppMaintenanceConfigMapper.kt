package piuk.blockchain.android.maintenance.data.mapper

import com.blockchain.mapper.Mapper
import com.google.android.play.core.appupdate.AppUpdateInfo
import piuk.blockchain.android.maintenance.data.model.AppMaintenanceConfigDto
import piuk.blockchain.android.maintenance.domain.model.AppMaintenanceConfig

internal object AppMaintenanceConfigMapper :
    Mapper<Pair<AppUpdateInfo, AppMaintenanceConfigDto>, AppMaintenanceConfig> {

    override fun map(type: Pair<AppUpdateInfo, AppMaintenanceConfigDto>): AppMaintenanceConfig {
        return type.let { (appUpdateInfo, maintenanceConfig) ->
            AppMaintenanceConfig(
                playStoreVersion = appUpdateInfo.availableVersionCode(),
                bannedVersions = maintenanceConfig.bannedVersions,
                minimumAppVersion = maintenanceConfig.minimumAppVersion,
                softUpgradeVersion = maintenanceConfig.softUpgradeVersion,
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