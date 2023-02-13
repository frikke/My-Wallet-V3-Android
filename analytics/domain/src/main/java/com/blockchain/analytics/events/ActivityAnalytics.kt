package com.blockchain.analytics.events

import com.blockchain.analytics.AnalyticsEvent

fun transactionsShown(activityType: String): AnalyticsEvent = object : AnalyticsEvent {
    override val event: String = "activity_page_shown"
    override val params: Map<String, String> = mapOf(
        "wallet" to activityType
    )
}
