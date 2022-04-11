package piuk.blockchain.android.ui.maintenance.domain.model

import kotlinx.serialization.Serializable

/**
 *
 * @property softUpgradeVersion if current version is earlier than this, we will show an optional update prompt
 * @property siteWideMaintenance everything is under maintenance, will block all usage of the app
 * @property skippedSoftVersion latest [softUpgradeVersion] that was skipped
 */
@Serializable
data class AppMaintenanceConfig(
    val playStoreVersion: Int,
    val bannedVersions: List<Int>,
    val softUpgradeVersion: Int,
    val skippedSoftVersion: Int,
    val minimumOSVersion: Int,
    val siteWideMaintenance: Boolean,
    val statusURL: String,
    val storeURI: String,
    val websiteUrl: String
)