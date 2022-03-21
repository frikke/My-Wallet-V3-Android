package piuk.blockchain.android.ui.kyc.veriffsplash

import com.blockchain.nabu.Tier
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import java.io.Serializable

sealed class VeriffAnalytics(override val event: String, override val params: Map<String, Serializable> = emptyMap()) :
    AnalyticsEvent {
    class VerifSubmissionFailed(
        tierUserIsAboutToUpgrade: Tier,
        failureReason: String
    ) : VeriffAnalytics(
        event = AnalyticsNames.VERIFICATION_SUBMISSION_FAILED.eventName,
        params = mapOf(
            "failure_reason" to failureReason,
            "tier" to tierUserIsAboutToUpgrade.ordinal,
            "provider" to "BLOCKCHAIN"
        )
    )
}
