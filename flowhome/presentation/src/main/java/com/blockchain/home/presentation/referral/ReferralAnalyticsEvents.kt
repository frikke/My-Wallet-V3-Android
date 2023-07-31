package com.blockchain.home.presentation.referral

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames

sealed class ReferralAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    data class ReferralView(val campaign: String) : ReferralAnalyticsEvents(
        event = AnalyticsNames.REFERRAL_VIEW_REFERRAL.eventName,
        params = mapOf(
            KEY_PLATFORM to WALLET,
            KEY_CAMPAIGN_ID to campaign
        )
    )

    data class ReferralShareCode(val code: String, val campaign: String) : ReferralAnalyticsEvents(
        event = AnalyticsNames.REFERRAL_SHARE_CODE.eventName,
        params = mapOf(
            KEY_PLATFORM to WALLET,
            KEY_CAMPAIGN_ID to campaign,
            KEY_CODE to code
        )
    )

    data class ReferralCopyCode(val code: String, val campaign: String) : ReferralAnalyticsEvents(
        event = AnalyticsNames.REFERRAL_COPY_CODE.eventName,
        params = mapOf(
            KEY_PLATFORM to WALLET,
            KEY_CAMPAIGN_ID to campaign,
            KEY_CODE to code
        )
    )

    companion object {
        private const val WALLET = "wallet"
        private const val KEY_PLATFORM = "platform"
        private const val KEY_CAMPAIGN_ID = "campaign_id"
        private const val KEY_CODE = "code"
    }
}
