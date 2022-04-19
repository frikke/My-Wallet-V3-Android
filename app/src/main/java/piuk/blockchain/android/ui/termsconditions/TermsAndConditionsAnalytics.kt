package piuk.blockchain.android.ui.termsconditions

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import java.io.Serializable

sealed class TermsAndConditionsAnalytics(
    override val event: String,
    override val params: Map<String, Serializable> = emptyMap()
) : AnalyticsEvent {
    object Viewed : TermsAndConditionsAnalytics(AnalyticsNames.TERMS_CONDITIONS_VIEWED.eventName)
    object Accepted : TermsAndConditionsAnalytics(AnalyticsNames.TERMS_CONDITIONS_ACCEPTED.eventName)
}
