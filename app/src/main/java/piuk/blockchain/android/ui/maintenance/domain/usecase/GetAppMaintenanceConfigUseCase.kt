package piuk.blockchain.android.ui.maintenance.domain.usecase

import com.blockchain.outcome.fold
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.ui.maintenance.domain.model.AppMaintenanceStatus
import piuk.blockchain.android.ui.maintenance.domain.repository.AppMaintenanceRepository

class GetAppMaintenanceConfigUseCase(private val repository: AppMaintenanceRepository) {
    suspend operator fun invoke(): AppMaintenanceStatus = repository.getAppMaintenanceConfig().fold(
        onFailure = { AppMaintenanceStatus.NonActionable.Unknown },
        onSuccess = { config ->

            val currentVersion = BuildConfig.VERSION_CODE

            if (config.siteWideMaintenance) {
                AppMaintenanceStatus.Actionable.SiteWideMaintenance(config.statusURL)
            } else if (config.bannedVersions.contains(currentVersion)) {
                if (config.bannedVersions.contains(config.playStoreVersion)) {
                    AppMaintenanceStatus.Actionable.RedirectToWebsite(config.websiteUrl)
                } else {
                    AppMaintenanceStatus.Actionable.MandatoryUpdate(config.storeURI)
                }
            } else if (currentVersion < config.softUpgradeVersion && config.softUpgradeVersion != config.skippedSoftVersion) {
                AppMaintenanceStatus.Actionable.OptionalUpdate(config.storeURI)
            } else {
                AppMaintenanceStatus.NonActionable.AllClear
            }

            AppMaintenanceStatus.Actionable.OptionalUpdate(config.storeURI)
        }
    )
}