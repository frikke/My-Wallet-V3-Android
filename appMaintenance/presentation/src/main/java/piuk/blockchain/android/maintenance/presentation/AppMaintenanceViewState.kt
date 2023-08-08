package piuk.blockchain.android.maintenance.presentation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import piuk.blockchain.android.maintenance.domain.model.AppMaintenanceStatus

enum class AppMaintenanceViewState(
    @DrawableRes val image: Int?,
    @StringRes val title: Int,
    @StringRes val description: Int,
    val button1: AppMaintenanceButtonSettings?,
    val button2: AppMaintenanceButtonSettings?
) : ViewState {
    NO_STATUS(
        image = null,
        title = com.blockchain.stringResources.R.string.empty,
        description = com.blockchain.stringResources.R.string.empty,
        button1 = null,
        button2 = null
    ),
    OS_NOT_SUPPORTED(
        image = R.drawable.ic_maintenance,
        title = com.blockchain.stringResources.R.string.app_maintenance_os_not_supported_title,
        description = com.blockchain.stringResources.R.string.app_maintenance_os_not_supported_description,
        button1 = AppMaintenanceButtonSettings(
            buttonText = com.blockchain.stringResources.R.string.app_maintenance_cta_redirect_website,
            intent = AppMaintenanceIntents.OSNotSupported
        ),
        button2 = null
    ),
    SITE_WIDE_MAINTENANCE(
        image = R.drawable.ic_maintenance,
        title = com.blockchain.stringResources.R.string.app_maintenance_down_title,
        description = com.blockchain.stringResources.R.string.app_maintenance_site_wide_description,
        button1 = AppMaintenanceButtonSettings(
            buttonText = com.blockchain.stringResources.R.string.app_maintenance_cta_view_status,
            intent = AppMaintenanceIntents.ViewStatus
        ),
        button2 = null
    ),
    REDIRECT_TO_WEBSITE(
        image = R.drawable.ic_maintenance,
        title = com.blockchain.stringResources.R.string.app_maintenance_down_title,
        description = com.blockchain.stringResources.R.string.app_maintenance_redirect_website_description,
        button1 = null,
        button2 = AppMaintenanceButtonSettings(
            buttonText = com.blockchain.stringResources.R.string.app_maintenance_cta_redirect_website,
            intent = AppMaintenanceIntents.RedirectToWebsite
        )
    ),
    MANDATORY_UPDATE(
        image = R.drawable.ic_outdated_app,
        title = com.blockchain.stringResources.R.string.app_maintenance_update_title,
        description = com.blockchain.stringResources.R.string.app_maintenance_update_description,
        button1 = null,
        button2 = AppMaintenanceButtonSettings(
            buttonText = com.blockchain.stringResources.R.string.app_maintenance_cta_update_now,
            intent = AppMaintenanceIntents.UpdateApp
        )
    ),
    OPTIONAL_UPDATE(
        image = R.drawable.ic_outdated_app,
        title = com.blockchain.stringResources.R.string.app_maintenance_update_title,
        description = com.blockchain.stringResources.R.string.app_maintenance_update_description,
        button1 = AppMaintenanceButtonSettings(
            buttonText = com.blockchain.stringResources.R.string.app_maintenance_cta_update_later,
            intent = AppMaintenanceIntents.SkipUpdate
        ),
        button2 = AppMaintenanceButtonSettings(
            buttonText = com.blockchain.stringResources.R.string.app_maintenance_cta_update_now,
            intent = AppMaintenanceIntents.UpdateApp
        )
    );

    companion object {
        fun fromStatus(status: AppMaintenanceStatus.Actionable?) = when (status) {
            is AppMaintenanceStatus.Actionable.OSNotSupported -> OS_NOT_SUPPORTED
            is AppMaintenanceStatus.Actionable.SiteWideMaintenance -> SITE_WIDE_MAINTENANCE
            is AppMaintenanceStatus.Actionable.RedirectToWebsite -> REDIRECT_TO_WEBSITE
            is AppMaintenanceStatus.Actionable.MandatoryUpdate -> MANDATORY_UPDATE
            is AppMaintenanceStatus.Actionable.OptionalUpdate -> OPTIONAL_UPDATE
            else -> NO_STATUS
        }
    }
}

data class AppMaintenanceButtonSettings(
    @StringRes val buttonText: Int,
    val intent: AppMaintenanceIntents
)
