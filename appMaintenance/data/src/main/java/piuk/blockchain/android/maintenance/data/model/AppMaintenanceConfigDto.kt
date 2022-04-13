package piuk.blockchain.android.maintenance.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AppMaintenanceConfigDto(
    @SerialName("bannedVersions") val bannedVersions: List<Int>,
    @SerialName("minimumAppVersion") val minimumAppVersion: Int,
    @SerialName("softUpgradeVersion") val softUpgradeVersion: Int,
    @SerialName("minimumOSVersion") val minimumOSVersion: Int,
    @SerialName("sitewideMaintenance") val siteWideMaintenance: Boolean,
    @SerialName("redirectToWebsite") val redirectToWebsite: Boolean,
    @SerialName("statusURL") val statusUrl: String,
    @SerialName("storeURI") val storeUrl: String,
    @SerialName("websiteUrl") val websiteUrl: String
)
