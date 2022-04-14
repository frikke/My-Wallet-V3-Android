package piuk.blockchain.android.maintenance.domain.model

/**
 *
 * @property minimumAppVersion mandatory update if the current version is earlier than this
 * @property softUpgradeVersion if current version is earlier than this, we will show an optional update prompt
 * @property siteWideMaintenance everything is under maintenance, will block all usage of the app
 * @property storeUrl if this is empty the update will be in-app, if not we redirect to the url for download
 */
data class AppMaintenanceConfig(
    val currentVersionCode: Int,
    val currentOsVersion: Int,
    val playStoreVersion: Int,
    val bannedVersions: List<Int>,
    val minimumAppVersion: Int,
    val softUpgradeVersion: Int,
    val minimumOSVersion: Int,
    val siteWideMaintenance: Boolean,
    val redirectToWebsite: Boolean,
    val statusUrl: String,
    val storeUrl: String,
    val websiteUrl: String
)
