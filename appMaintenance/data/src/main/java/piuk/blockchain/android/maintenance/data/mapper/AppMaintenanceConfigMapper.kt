package piuk.blockchain.android.maintenance.data.mapper

import com.google.android.play.core.appupdate.AppUpdateInfo
import piuk.blockchain.android.maintenance.data.model.AppMaintenanceConfigDto
import piuk.blockchain.android.maintenance.domain.model.AppMaintenanceConfig

internal object AppMaintenanceConfigMapper {
    fun map(
        appUpdateInfo: AppUpdateInfo,
        maintenanceConfig: AppMaintenanceConfigDto,
        currentVersionCode: Int,
        currentOsVersion: Int
    ): AppMaintenanceConfig {
        return AppMaintenanceConfig(
            currentVersionCode = currentVersionCode,
            currentOsVersion = currentOsVersion,
            playStoreVersion = appUpdateInfo.availableVersionCode(),
            bannedVersions = maintenanceConfig.bannedVersions,
            minimumAppVersion = maintenanceConfig.minimumAppVersion,
            softUpgradeVersion = maintenanceConfig.softUpgradeVersion,
            minimumOSVersion = maintenanceConfig.minimumOSVersion,
            siteWideMaintenance = maintenanceConfig.siteWideMaintenance,
            redirectToWebsite = maintenanceConfig.redirectToWebsite,
            statusUrl = maintenanceConfig.statusUrl,
            storeUrl = maintenanceConfig.storeUrl,
            websiteUrl = maintenanceConfig.websiteUrl
        )
    }
}
