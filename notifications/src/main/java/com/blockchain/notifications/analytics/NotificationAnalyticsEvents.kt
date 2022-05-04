package com.blockchain.notifications.analytics

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import java.io.Serializable

sealed class NotificationAnalyticsEvents(
    override val event: String,
    override val params: Map<String, Serializable>,
) : AnalyticsEvent {

    data class PushNotificationReceived(
        val payload: Serializable
    ) : NotificationAnalyticsEvents(
        AnalyticsNames.PUSH_NOTIFICATION_RECEIVED.eventName,
        mapOf(KEY_CAMPAIGN to payload)
    )

    data class PushNotificationTapped(
        val payload: Serializable?
    ) : NotificationAnalyticsEvents(
        AnalyticsNames.PUSH_NOTIFICATION_TAPPED.eventName,
        mapOf(KEY_CAMPAIGN to (payload ?: ""))
    )

    companion object {
        private const val KEY_CAMPAIGN = "campaign"

        private const val KEY_CONTENT = "content"
        private const val KEY_MEDIUM = "medium"
        private const val KEY_NAME = "name"
        private const val KEY_SOURCE = "source"

        fun createCampaignPayload(payload: Map<String, String?>, title: String?): Serializable {
            val campaignData = hashMapOf<String, String>()
            val keys = listOf(KEY_CONTENT, KEY_MEDIUM, KEY_NAME, KEY_SOURCE)
            keys.forEach { key ->
                payload[key]?.let { campaignData[key] = it }
            }

            if (!campaignData.containsKey(KEY_NAME) && title != null) {
                campaignData[KEY_NAME] = title
            }

            return campaignData
        }
    }
}
