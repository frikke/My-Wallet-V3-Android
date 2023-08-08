package com.blockchain.transactions.upsell.buy

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import java.io.Serializable

object UpsellBuyViewed : AnalyticsEvent {
    override val event: String = AnalyticsNames.BUY_ASSET_UPSELL_PAGE_VIEWED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}

object UpsellBuyDismissed : AnalyticsEvent {
    override val event: String = AnalyticsNames.BUY_ASSET_UPSELL_PAGE_DISMISSED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}

object UpsellBuyMaybeLaterClicked : AnalyticsEvent {
    override val event: String = AnalyticsNames.BUY_ASSET_UPSELL_MAYBE_LATER_CLICKED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}

class UpsellBuyMostPopularClicked(
    val currency: String
) : AnalyticsEvent {
    override val event: String = AnalyticsNames.BUY_ASSET_UPSELL_MOST_POPULAR_ASSET_CLICKED.eventName
    override val params: Map<String, Serializable> = mapOf(
        "currency" to currency
    )
}
