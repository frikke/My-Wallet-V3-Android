package piuk.blockchain.android.ui.maintenance.domain.model

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