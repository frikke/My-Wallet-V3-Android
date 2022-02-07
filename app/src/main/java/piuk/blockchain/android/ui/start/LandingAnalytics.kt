package piuk.blockchain.android.ui.start

import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import com.blockchain.notifications.analytics.LaunchOrigin

sealed class LandingAnalytics(
    override val event: String,
    override val origin: LaunchOrigin?,
    override val params: Map<String, String> = mapOf()
) : AnalyticsEvent {
    object LogInClicked : LandingAnalytics(
        event = AnalyticsNames.LANDING_CTA_LOGIN_CLICKED.eventName,
        origin = LaunchOrigin.NUX_LAUNCH_PROMO_LOG_IN
    )
    object BuyCryptoCtaClicked : LandingAnalytics(
        event = AnalyticsNames.LANDING_CTA_SIGNUP_CLICKED.eventName,
        origin = LaunchOrigin.NUX_LAUNCH_PROMO_BUY_CRYPTO
    )
}
