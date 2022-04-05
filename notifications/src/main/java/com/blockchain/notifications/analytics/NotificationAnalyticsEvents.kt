package com.blockchain.notifications.analytics

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
