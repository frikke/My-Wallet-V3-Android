package com.blockchain.analytics.events

import com.blockchain.analytics.AnalyticsEvent

@Deprecated("Analytics events should be defined near point of use")
sealed class TransactionsAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = mapOf()
) : AnalyticsEvent {

    object TabItemClick : TransactionsAnalyticsEvents("transactions_tab_item_click")
}
