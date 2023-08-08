package piuk.blockchain.android.kyc

import com.blockchain.analytics.AnalyticsEvent

enum class KycAnalytics(override val event: String, override val params: Map<String, String> = emptyMap()) :
    AnalyticsEvent {
    UPGRADE_TO_GOLD_CLICKED("upgrade_to_gold_clicked"),
    UPGRADE_TO_GOLD_SEEN("upgrade_to_gold_seen")
}
