package piuk.blockchain.android.ui.maintenance.domain.model

sealed interface AppMaintenanceStatus {
    object Unknown : AppMaintenanceStatus
    object AllClear : AppMaintenanceStatus
    data class SiteWideMaintenance(val statusUrl: String) : AppMaintenanceStatus
    object RedirectToWebsite : AppMaintenanceStatus
    data class MandatoryUpdate(val playstoreUrl: String?) : AppMaintenanceStatus
    data class OptionalUpdate(val playstoreUrl: String?) : AppMaintenanceStatus
}