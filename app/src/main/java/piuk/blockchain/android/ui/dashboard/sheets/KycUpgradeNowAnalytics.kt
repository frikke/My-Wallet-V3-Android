package piuk.blockchain.android.ui.dashboard.sheets

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.nabu.Tier
import java.io.Serializable

class KycUpgradeNowViewed(val tier: Tier, isSdd: Boolean) : AnalyticsEvent {
    override val event: String = AnalyticsNames.KYC_UPGRADE_NOW_VIEWED.eventName
    override val params: Map<String, Serializable> = mapOf(
        "tier" to tier.toAnalyticsValue(isSdd)
    )
}

class KycUpgradeNowGetBasicClicked(val tier: Tier, isSdd: Boolean) : AnalyticsEvent {
    override val event: String = AnalyticsNames.KYC_UPGRADE_NOW_GET_BASIC_CLICKED.eventName
    override val params: Map<String, Serializable> = mapOf(
        "tier" to tier.toAnalyticsValue(isSdd)
    )
}

class KycUpgradeNowGetVerifiedClicked(val tier: Tier, isSdd: Boolean) : AnalyticsEvent {
    override val event: String = AnalyticsNames.KYC_UPGRADE_NOW_GET_VERIFIED_CLICKED.eventName
    override val params: Map<String, Serializable> = mapOf(
        "tier" to tier.toAnalyticsValue(isSdd)
    )
}

class KycUpgradeNowDismissed(val tier: Tier, isSdd: Boolean) : AnalyticsEvent {
    override val event: String = AnalyticsNames.KYC_UPGRADE_NOW_DISMISSED.eventName
    override val params: Map<String, Serializable> = mapOf(
        "tier" to tier.toAnalyticsValue(isSdd)
    )
}

private fun Tier.toAnalyticsValue(isSdd: Boolean): Int = when (this) {
    Tier.BRONZE -> 0
    Tier.SILVER -> if (isSdd) 3 else 1
    Tier.GOLD -> 4
}
