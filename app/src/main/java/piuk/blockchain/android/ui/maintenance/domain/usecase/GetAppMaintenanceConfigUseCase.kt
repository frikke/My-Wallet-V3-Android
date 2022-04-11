package piuk.blockchain.android.ui.maintenance.domain.usecase

import com.blockchain.extensions.exhaustive
import com.blockchain.outcome.fold
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.ui.maintenance.domain.model.AppMaintenanceStatus
import piuk.blockchain.android.ui.maintenance.domain.repository.AppMaintenanceRepository

class GetAppMaintenanceConfigUseCase(private val repository: AppMaintenanceRepository) {
    suspend operator fun invoke(): AppMaintenanceStatus {
        return repository.getAppMaintenanceConfig().fold(
            onFailure = { AppMaintenanceStatus.NonActionable.Unknown },
            onSuccess = { config ->
                val currentVersion = BuildConfig.VERSION_CODE // todo

                when {
                    config.siteWideMaintenance -> {
                        AppMaintenanceStatus.Actionable.SiteWideMaintenance(config.statusURL)
                    }

                    config.bannedVersions.contains(currentVersion) -> {
                        if (config.bannedVersions.contains(config.playStoreVersion)) {
                            AppMaintenanceStatus.Actionable.RedirectToWebsite(config.websiteUrl)
                        } else {
                            AppMaintenanceStatus.Actionable.MandatoryUpdate(config.storeURI)
                        }
                    }

                    currentVersion < config.softUpgradeVersion
                        && config.softUpgradeVersion != config.skippedSoftVersion -> {
                        AppMaintenanceStatus.Actionable.OptionalUpdate(config.softUpgradeVersion, config.storeURI)
                    }

                    else -> {
                        AppMaintenanceStatus.NonActionable.AllClear
                    }
                }.exhaustive
            }
        )
    }
}