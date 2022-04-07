package piuk.blockchain.android.ui.maintenance.domain.model

/**
 * @property bannedVersions List<Int>
 * @property softUpgradeVersion Int
 * @property minimumOSVersion Int
 * @property siteWideMaintenance Boolean
 * @property statusURL String
 * @property storeURI String
 */
data class AppMaintenanceConfig(
    val playStoreVersion: Int,
    val bannedVersions: List<Int>,
    val softUpgradeVersion: Int,
    val skippedSoftVersion: Int,
    val minimumOSVersion: Int,
    val siteWideMaintenance: Boolean,
    val statusURL: String,
    val storeURI: String,
)