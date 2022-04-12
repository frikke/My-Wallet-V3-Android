package piuk.blockchain.android.ui.maintenance.domain.usecase

import com.blockchain.extensions.exhaustive
import com.blockchain.outcome.fold
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.ui.maintenance.domain.model.AppMaintenanceStatus
import piuk.blockchain.android.ui.maintenance.domain.model.UpdateLocation
import piuk.blockchain.android.ui.maintenance.domain.repository.AppMaintenanceRepository

class GetAppMaintenanceConfigUseCase(private val repository: AppMaintenanceRepository) {
    suspend operator fun invoke(): AppMaintenanceStatus {
        return repository.getAppMaintenanceConfig().fold(
            onFailure = { AppMaintenanceStatus.NonActionable.Unknown },
            onSuccess = { config ->
                with(config) {
                    val currentVersion = BuildConfig.VERSION_CODE // todo

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

                        // if soft version is preferred and is not yet skipped by the user
                        currentVersion < softUpgradeVersion && softUpgradeVersion != skippedSoftVersion -> {
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