package com.blockchain.notifications.analytics

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames

sealed class NotificationAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap(),
) : AnalyticsEvent {

    data class PushNotificationReceived(val title: String, val content: String) :
        NotificationAnalyticsEvents(
            AnalyticsNames.PUSH_NOTIFICATION_RECEIVED.eventName,
            mapOf(KEY_NAME to title, KEY_CONTENT to content)
        )

    object PushNotificationTapped : NotificationAnalyticsEvents(AnalyticsNames.PUSH_NOTIFICATION_TAPPED.eventName)

    companion object {
        private const val KEY_NAME = "name"
        private const val KEY_CONTENT = "content"
    }
}
