package com.blockchain.analytics.events

import com.blockchain.analytics.AnalyticsEvent

@Deprecated("Analytics events should be defined near point of use")
sealed class AddressAnalytics(
    override val event: String,
    override val params: Map<String, String> = mapOf()
) : AnalyticsEvent {

    object ImportBTCAddress : AddressAnalytics(IMPORT_BTC_ADDRESS)

    companion object {
        private const val IMPORT_BTC_ADDRESS = "import"
    }
}
