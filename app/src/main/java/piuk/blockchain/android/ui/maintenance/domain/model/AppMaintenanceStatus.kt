package piuk.blockchain.android.ui.maintenance.domain.model

sealed interface AppMaintenanceStatus {
    /**
     * Status that would not require user interaction
     */
    sealed interface NonActionable : AppMaintenanceStatus {
        object Unknown : NonActionable
        object AllClear : NonActionable
    }

    /**
     * Status that would require user interaction
     */
    sealed interface Actionable : AppMaintenanceStatus {
        data class SiteWideMaintenance(val statusUrl: String) : Actionable
        data class RedirectToWebsite(val website: String) : Actionable
        data class MandatoryUpdate(val updateLocation: UpdateLocation) : Actionable
        data class OptionalUpdate(val softVersionCode: Int, val updateLocation: UpdateLocation) : Actionable
    }
}

sealed interface UpdateLocation {
    object InAppUpdate : UpdateLocation
    data class ExternalUrl(val playStoreUrl: String) : UpdateLocation

    companion object {
        fun fromUrl(url: String?) = if (url.isNullOrBlank()) InAppUpdate else ExternalUrl(url)
    }
}