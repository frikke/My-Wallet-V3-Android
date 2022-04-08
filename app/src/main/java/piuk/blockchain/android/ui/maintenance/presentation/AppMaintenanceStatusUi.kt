package piuk.blockchain.android.ui.maintenance.presentation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.maintenance.domain.model.AppMaintenanceStatus

enum class AppMaintenanceStatusUi(
    @DrawableRes val image: Int,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val button1Text: Int?,
    @StringRes val button2Text: Int?
) {
    NO_STATUS(
        image = R.drawable.ic_outdated_app,
        title = R.string.empty,
        description = R.string.empty,
        button1Text = null,
        button2Text = null
    ),
    SITE_WIDE_MAINTENANCE(
        image = R.drawable.ic_down_for_maintenance,
        title = R.string.app_maintenance_down_title,
        description = R.string.app_maintenance_site_wide_description,
        button1Text = R.string.app_maintenance_cta_view_status,
        button2Text = null
    ),
    REDIRECT_TO_WEBSITE(
        image = R.drawable.ic_down_for_maintenance,
        title = R.string.app_maintenance_down_title,
        description = R.string.app_maintenance_redirect_website_description,
        button1Text = null,
        button2Text = R.string.app_maintenance_cta_redirect_website
    ),
    MANDATORY_UPDATE(
        image = R.drawable.ic_outdated_app,
        title = R.string.app_maintenance_update_title,
        description = R.string.app_maintenance_update_description,
        button1Text = null,
        button2Text = R.string.app_maintenance_cta_update_now
    ),
    OPTIONAL_UPDATE(
        image = R.drawable.ic_outdated_app,
        title = R.string.app_maintenance_update_title,
        description = R.string.app_maintenance_update_description,
        button1Text = R.string.app_maintenance_cta_update_later,
        button2Text = R.string.app_maintenance_cta_update_now
    );

    companion object {
        fun fromStatus(status: AppMaintenanceStatus.Actionable) = when (status) {
            is AppMaintenanceStatus.Actionable.SiteWideMaintenance -> SITE_WIDE_MAINTENANCE
            is AppMaintenanceStatus.Actionable.RedirectToWebsite -> REDIRECT_TO_WEBSITE
            is AppMaintenanceStatus.Actionable.MandatoryUpdate -> MANDATORY_UPDATE
            is AppMaintenanceStatus.Actionable.OptionalUpdate -> OPTIONAL_UPDATE
        }
    }
}