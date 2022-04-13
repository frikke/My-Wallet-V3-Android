package com.blockchain.analytics.events

import com.blockchain.analytics.AnalyticsEvent

@Deprecated("Analytics events should be defined near point of use")
sealed class SendAnalytics(override val event: String, override val params: Map<String, String> = mapOf()) :
    AnalyticsEvent {
    object QRButtonClicked : SendAnalytics("send_form_qr_button_click")
}
