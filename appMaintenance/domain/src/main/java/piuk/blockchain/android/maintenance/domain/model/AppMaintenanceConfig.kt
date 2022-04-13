package piuk.blockchain.android.maintenance.domain.model

/**
 *
 * @property softUpgradeVersion if current version is earlier than this, we will show an optional update prompt
 * @property siteWideMaintenance everything is under maintenance, will block all usage of the app
 */
data class AppMaintenanceConfig(
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