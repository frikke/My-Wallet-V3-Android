package com.blockchain.analytics.events

import com.blockchain.analytics.AnalyticsEvent

@Deprecated("Analytics events should be defined near point of use")
sealed class NotificationAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap(),
) : AnalyticsEvent {

    object PushNotificationReceived :
        NotificationAnalyticsEvents(
            AnalyticsNames.PUSH_NOTIFICATION_RECEIVED.eventName,
            emptyMap()
        )

    object PushNotificationTapped :
        NotificationAnalyticsEvents(
            AnalyticsNames.PUSH_NOTIFICATION_TAPPED.eventName,
            emptyMap()
        )
}
