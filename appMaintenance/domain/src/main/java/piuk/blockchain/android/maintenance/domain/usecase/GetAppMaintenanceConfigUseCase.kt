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
                    val currentVersion = 16830 //BuildConfig.VERSION_CODE // todo

                    when {
                        siteWideMaintenance -> {
                            AppMaintenanceStatus.Actionable.SiteWideMaintenance(statusUrl)
                        }

                        redirectToWebsite -> {
                            AppMaintenanceStatus.Actionable.RedirectToWebsite(websiteUrl)
                        }

                        // redirect to website when current app and playstore are both broken, ie no healthy update available
                        // if playstore is healthy, force an update
                        currentVersion in bannedVersions -> {
                            if (playStoreVersion in bannedVersions) {
                                AppMaintenanceStatus.Actionable.RedirectToWebsite(websiteUrl)
                            } else {
                                AppMaintenanceStatus.Actionable.MandatoryUpdate(UpdateLocation.fromUrl(storeUrl))
                            }
                        }

                        currentVersion < minimumAppVersion -> {
                            AppMaintenanceStatus.Actionable.MandatoryUpdate(UpdateLocation.fromUrl(storeUrl))
                        }

                        // if soft version is preferred
                        currentVersion < softUpgradeVersion -> {
                            AppMaintenanceStatus.Actionable.OptionalUpdate(
                                softUpgradeVersion,
                                UpdateLocation.fromUrl(storeUrl)
                            )
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