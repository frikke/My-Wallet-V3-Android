package com.blockchain.transactions.upsell

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import java.io.Serializable

object UpSellAnotherAssetViewed : AnalyticsEvent {
    override val event: String = AnalyticsNames.BUY_ASSET_UPSELL_PAGE_VIEWED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}

object UpSellAnotherAssetDismissed : AnalyticsEvent {
    override val event: String = AnalyticsNames.BUY_ASSET_UPSELL_PAGE_DISMISSED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}

object UpSellAnotherAssetMaybeLaterClicked : AnalyticsEvent {
    override val event: String = AnalyticsNames.BUY_ASSET_UPSELL_MAYBE_LATER_CLICKED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}

class UpSellAnotherAssetMostPopularClicked(
    val currency: String
) : AnalyticsEvent {
    override val event: String = AnalyticsNames.BUY_ASSET_UPSELL_MOST_POPULAR_ASSET_CLICKED.eventName
    override val params: Map<String, Serializable> = mapOf(
        "currency" to currency
    )
}
