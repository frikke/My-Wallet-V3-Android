package com.blockchain.notifications.analytics

import android.os.Bundle
import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames

sealed class NotificationAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String>,
) : AnalyticsEvent {

    data class PushNotificationReceived(
        val campaignData: Map<String, String>
    ) : NotificationAnalyticsEvents(
        AnalyticsNames.PUSH_NOTIFICATION_RECEIVED.eventName,
        campaignData.mapKeys { "${KEY_CAMPAIGN}_$it" }
    )

    data class PushNotificationTapped(
        val campaignData: Map<String, String>
    ) : NotificationAnalyticsEvents(
        AnalyticsNames.PUSH_NOTIFICATION_TAPPED.eventName,
        campaignData.mapKeys { "${KEY_CAMPAIGN}_$it" }
    )

    companion object {
        private const val KEY_CAMPAIGN = "campaign"

        private const val KEY_CONTENT = "content"
        private const val KEY_MEDIUM = "medium"
        private const val KEY_NAME = "name"
        private const val KEY_SOURCE = "source"
        private const val KEY_TEMPLATE = "template"

        private val ANALYTICS_KEYS = listOf(KEY_CONTENT, KEY_MEDIUM, KEY_NAME, KEY_SOURCE, KEY_TEMPLATE)

        fun createCampaignPayload(payload: Map<String, String?>, title: String?): Map<String, String> {
            val campaignData = hashMapOf<String, String>()
            ANALYTICS_KEYS.forEach { key ->
                payload[key]?.let {
                    campaignData[key] = it
                }
            }
            if (!campaignData.containsKey(KEY_NAME) && title != null) {
                campaignData[KEY_NAME] = title
            }
            return campaignData
        }

        fun createCampaignPayload(bundle: Bundle?): Map<String, String> {
            return ANALYTICS_KEYS.mapNotNull { key ->
                bundle?.getString(key)?.let { value ->
                    Pair(key, value)
                }
            }.toMap()
        }
    }
}
