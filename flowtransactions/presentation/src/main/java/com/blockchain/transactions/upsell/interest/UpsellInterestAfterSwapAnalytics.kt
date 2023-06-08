package com.blockchain.transactions.upsell.interest

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import java.io.Serializable

class UpsellInterestAfterSwapViewed(
    val currency: String
) : AnalyticsEvent {
    override val event: String = AnalyticsNames.INTEREST_UPSELL_AFTER_SWAP_PAGE_VIEWED.eventName
    override val params: Map<String, Serializable> = mapOf(
        "currency" to currency
    )
}

class UpsellInterestAfterSwapDismissed(
    val currency: String
) : AnalyticsEvent {
    override val event: String = AnalyticsNames.INTEREST_UPSELL_AFTER_SWAP_PAGE_VIEWED.eventName
    override val params: Map<String, Serializable> = mapOf(
        "currency" to currency
    )
}

class UpsellInterestAfterSwapMaybeLaterClicked(
    val currency: String
) : AnalyticsEvent {
    override val event: String = AnalyticsNames.INTEREST_UPSELL_AFTER_SWAP_MAYBE_LATER_CLICKED.eventName
    override val params: Map<String, Serializable> = mapOf(
        "currency" to currency
    )
}

class UpsellInterestAfterSwapStartEarningClicked(
    val currency: String
) : AnalyticsEvent {
    override val event: String = AnalyticsNames.INTEREST_UPSELL_AFTER_SWAP_START_EARNING_CLICKED.eventName
    override val params: Map<String, Serializable> = mapOf(
        "currency" to currency
    )
}
