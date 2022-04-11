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
        data class MandatoryUpdate(val playStoreUrl: String?) : Actionable
        data class OptionalUpdate(val softVersionCode: Int, val playStoreUrl: String?) : Actionable
    }
}