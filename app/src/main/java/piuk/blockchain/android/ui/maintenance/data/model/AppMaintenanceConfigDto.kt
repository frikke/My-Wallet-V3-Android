package piuk.blockchain.android.ui.maintenance.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import piuk.blockchain.android.ui.maintenance.domain.model.AppMaintenanceConfig

@Serializable
internal data class AppMaintenanceConfigDto(
    @SerialName("bannedVersions") val bannedVersions: List<Int>,
    @SerialName("softUpgradeVersion") val softUpgradeVersion: Int,
    @SerialName("minimumOSVersion") val minimumOSVersion: Int,
    @SerialName("sitewideMaintenance") val siteWideMaintenance: Boolean,
    @SerialName("statusURL") val statusURL: String,
    @SerialName("storeURI") val storeURI: String,
    @SerialName("websiteUrl") val websiteUrl: String
)
