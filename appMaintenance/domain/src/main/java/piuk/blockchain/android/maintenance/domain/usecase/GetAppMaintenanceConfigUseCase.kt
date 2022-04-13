package piuk.blockchain.android.maintenance.domain.usecase

import com.blockchain.extensions.exhaustive
import com.blockchain.outcome.fold
import piuk.blockchain.android.maintenance.domain.model.AppMaintenanceStatus
import piuk.blockchain.android.maintenance.domain.model.UpdateLocation
import piuk.blockchain.android.maintenance.domain.repository.AppMaintenanceRepository

class GetAppMaintenanceConfigUseCase(private val repository: AppMaintenanceRepository) {
    suspend operator fun invoke(): AppMaintenanceStatus {
        return repository.getAppMaintenanceConfig().fold(
            onFailure = { AppMaintenanceStatus.NonActionable.Unknown },
            onSuccess = { config ->
                with(config) {
                    when {
                        siteWideMaintenance -> {
                            AppMaintenanceStatus.Actionable.SiteWideMaintenance(statusUrl)
                        }

                        redirectToWebsite -> {
                            AppMaintenanceStatus.Actionable.RedirectToWebsite(websiteUrl)
                        }

                        // redirect to website when current app and playstore are both broken, ie no healthy update available
                        // if playstore is healthy, force an update
                        currentVersionCode in bannedVersions -> {
                            if (playStoreVersion in bannedVersions) {
                                AppMaintenanceStatus.Actionable.RedirectToWebsite(websiteUrl)
                            } else {
                                AppMaintenanceStatus.Actionable.MandatoryUpdate(UpdateLocation.fromUrl(storeUrl))
                            }
                        }

                        currentVersionCode < minimumAppVersion -> {
                            AppMaintenanceStatus.Actionable.MandatoryUpdate(UpdateLocation.fromUrl(storeUrl))
                        }

                        // if soft version is preferred
                        currentVersionCode < softUpgradeVersion -> {
                            AppMaintenanceStatus.Actionable.OptionalUpdate(UpdateLocation.fromUrl(storeUrl))
                        }

                        else -> {
                            AppMaintenanceStatus.NonActionable.AllClear
                        }
                    }.exhaustive
                }
            }
        )
    }
}