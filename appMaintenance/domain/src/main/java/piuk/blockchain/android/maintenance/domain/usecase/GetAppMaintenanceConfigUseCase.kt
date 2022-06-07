package piuk.blockchain.android.maintenance.domain.usecase

import com.blockchain.extensions.exhaustive
import com.blockchain.outcome.getOrDefault
import com.blockchain.outcome.map
import piuk.blockchain.android.maintenance.domain.model.AppMaintenanceStatus
import piuk.blockchain.android.maintenance.domain.model.UpdateLocation
import piuk.blockchain.android.maintenance.domain.repository.AppMaintenanceService

class GetAppMaintenanceConfigUseCase(private val service: AppMaintenanceService) {
    suspend operator fun invoke(): AppMaintenanceStatus {
        return service.getAppMaintenanceConfig()
            .map { config ->
                with(config) {
                    when {
                        currentOsVersion < minimumOSVersion -> {
                            AppMaintenanceStatus.Actionable.OSNotSupported(websiteUrl)
                        }

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
            .getOrDefault(AppMaintenanceStatus.NonActionable.Unknown)
    }
}
