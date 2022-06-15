package piuk.blockchain.android.maintenance.data.mapper

import com.google.android.play.core.appupdate.AppUpdateInfo
import piuk.blockchain.android.maintenance.data.model.AppMaintenanceConfigDto
import piuk.blockchain.android.maintenance.domain.model.AppMaintenanceConfig

internal object AppMaintenanceConfigMapper {
    /**
     * @param appUpdateInfo could be null in non-prod-release builds, or just as an edge case it fails to load,
     * so we use [AppMaintenanceConfigDto.playStoreVersion] instead of [AppUpdateInfo.availableVersionCode]
     * and external [AppMaintenanceConfigDto.inAppUpdateFallbackUrl] instead of the in-app update
     */
    fun map(
        appUpdateInfo: AppUpdateInfo?,
        maintenanceConfig: AppMaintenanceConfigDto,
        currentVersionCode: Int,
        currentOsVersion: Int
    ): AppMaintenanceConfig {
        return AppMaintenanceConfig(
            isRemoteConfigIgnored = false,
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

    /**
     * Creates a [AppMaintenanceConfig] object with default values
     * and [AppMaintenanceConfig.isRemoteConfigIgnored] true
     */
    fun mapIgnored() = AppMaintenanceConfig(
        isRemoteConfigIgnored = true,
        currentOsVersion = 0,
        currentVersionCode = 0,
        playStoreVersion = 0,
        bannedVersions = listOf(),
        minimumAppVersion = 0,
        softUpgradeVersion = 0,
        minimumOSVersion = 0,
        siteWideMaintenance = false,
        redirectToWebsite = false,
        statusUrl = "",
        storeUrl = "",
        websiteUrl = ""
    )
}
