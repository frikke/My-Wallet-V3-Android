package piuk.blockchain.android.maintenance.data.mapper

import com.google.android.play.core.appupdate.AppUpdateInfo
import piuk.blockchain.android.maintenance.data.model.AppMaintenanceConfigDto
import piuk.blockchain.android.maintenance.domain.model.AppMaintenanceConfig

internal object AppMaintenanceConfigMapper {
    /**
     * [appUpdateInfo] could be null in non-prod-release builds, or just as an edge case it fails to load,
     * so we use [AppMaintenanceConfigDto.playStoreVersion] instead
     */
    fun map(
        appUpdateInfo: AppUpdateInfo?,
        maintenanceConfig: AppMaintenanceConfigDto,
        currentVersionCode: Int,
        currentOsVersion: Int
    ): AppMaintenanceConfig {
        return AppMaintenanceConfig(
            currentVersionCode = currentVersionCode,
            currentOsVersion = currentOsVersion,
            playStoreVersion = appUpdateInfo?.availableVersionCode() ?: maintenanceConfig.playStoreVersion,
            bannedVersions = maintenanceConfig.bannedVersions,
            minimumAppVersion = maintenanceConfig.minimumAppVersion,
            softUpgradeVersion = maintenanceConfig.softUpgradeVersion,
            minimumOSVersion = maintenanceConfig.minimumOSVersion,
            siteWideMaintenance = maintenanceConfig.siteWideMaintenance,
            redirectToWebsite = maintenanceConfig.redirectToWebsite,
            statusUrl = maintenanceConfig.statusUrl,
            storeUrl = with(maintenanceConfig) {
                if (appUpdateInfo == null && storeUrl.isBlank()) inAppUpdateFallbackUrl
                else storeUrl
            },
            websiteUrl = maintenanceConfig.websiteUrl
        )
    }
}
