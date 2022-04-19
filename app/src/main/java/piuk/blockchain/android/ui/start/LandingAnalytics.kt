package piuk.blockchain.android.ui.start

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.analytics.events.LaunchOrigin

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
