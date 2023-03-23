package com.blockchain.presentation.customviews.kyc

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.core.kyc.domain.model.KycTier
import java.io.Serializable

class KycUpgradeNowViewed(val tier: KycTier) : AnalyticsEvent {
    override val event: String = AnalyticsNames.KYC_UPGRADE_NOW_VIEWED.eventName
    override val params: Map<String, Serializable> = mapOf(
        "tier" to tier.toAnalyticsValue()
    )
}

class KycUpgradeNowGetBasicClicked(val tier: KycTier) : AnalyticsEvent {
    override val event: String = AnalyticsNames.KYC_UPGRADE_NOW_GET_BASIC_CLICKED.eventName
    override val params: Map<String, Serializable> = mapOf(
        "tier" to tier.toAnalyticsValue()
    )
}

class KycUpgradeNowGetVerifiedClicked(val tier: KycTier) : AnalyticsEvent {
    override val event: String = AnalyticsNames.KYC_UPGRADE_NOW_GET_VERIFIED_CLICKED.eventName
    override val params: Map<String, Serializable> = mapOf(
        "tier" to tier.toAnalyticsValue()
    )
}

class KycUpgradeNowDismissed(val tier: KycTier) : AnalyticsEvent {
    override val event: String = AnalyticsNames.KYC_UPGRADE_NOW_DISMISSED.eventName
    override val params: Map<String, Serializable> = mapOf(
        "tier" to tier.toAnalyticsValue()
    )
}

private fun KycTier.toAnalyticsValue(): Int = when (this) {
    KycTier.BRONZE -> 0
    KycTier.SILVER -> 1
    KycTier.GOLD -> 4
}
