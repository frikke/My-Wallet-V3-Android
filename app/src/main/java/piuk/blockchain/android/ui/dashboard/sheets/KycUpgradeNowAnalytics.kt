package piuk.blockchain.android.ui.dashboard.sheets

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.core.kyc.domain.model.KycTier
import java.io.Serializable

class KycUpgradeNowViewed(val tier: KycTier, isSdd: Boolean) : AnalyticsEvent {
    override val event: String = AnalyticsNames.KYC_UPGRADE_NOW_VIEWED.eventName
    override val params: Map<String, Serializable> = mapOf(
        "tier" to tier.toAnalyticsValue(isSdd)
    )
}

class KycUpgradeNowGetBasicClicked(val tier: KycTier, isSdd: Boolean) : AnalyticsEvent {
    override val event: String = AnalyticsNames.KYC_UPGRADE_NOW_GET_BASIC_CLICKED.eventName
    override val params: Map<String, Serializable> = mapOf(
        "tier" to tier.toAnalyticsValue(isSdd)
    )
}

class KycUpgradeNowGetVerifiedClicked(val tier: KycTier, isSdd: Boolean) : AnalyticsEvent {
    override val event: String = AnalyticsNames.KYC_UPGRADE_NOW_GET_VERIFIED_CLICKED.eventName
    override val params: Map<String, Serializable> = mapOf(
        "tier" to tier.toAnalyticsValue(isSdd)
    )
}

class KycUpgradeNowDismissed(val tier: KycTier, isSdd: Boolean) : AnalyticsEvent {
    override val event: String = AnalyticsNames.KYC_UPGRADE_NOW_DISMISSED.eventName
    override val params: Map<String, Serializable> = mapOf(
        "tier" to tier.toAnalyticsValue(isSdd)
    )
}

private fun KycTier.toAnalyticsValue(isSdd: Boolean): Int = when (this) {
    KycTier.BRONZE -> 0
    KycTier.SILVER -> if (isSdd) 3 else 1
    KycTier.GOLD -> 4
}
