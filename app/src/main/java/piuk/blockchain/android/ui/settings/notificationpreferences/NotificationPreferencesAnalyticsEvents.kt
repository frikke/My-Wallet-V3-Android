package piuk.blockchain.android.ui.settings.notificationpreferences

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import piuk.blockchain.android.ui.settings.notificationpreferences.details.ContactMethod

sealed class NotificationPreferencesAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap(),
) : AnalyticsEvent {

    object NotificationClicked : NotificationPreferencesAnalyticsEvents(AnalyticsNames.NOTIFICATION_CLICKED.eventName)

    object NotificationViewed : NotificationPreferencesAnalyticsEvents(AnalyticsNames.NOTIFICATION_VIEWED.eventName)

    data class NotificationPreferencesTapped(val option: String) :
        NotificationPreferencesAnalyticsEvents(
            AnalyticsNames.NOTIFICATION_PREFERENCES_CLICKED.eventName,
            mapOf(KEY_OPTION_SELECTED to option)
        )

    data class NotificationOptionViewed(val option: String) :
        NotificationPreferencesAnalyticsEvents(
            AnalyticsNames.NOTIFICATION_PREFERENCES_VIEWED.eventName,
            mapOf(KEY_OPTION_VIEWED to option)
        )

    object NotificationsClosed : NotificationPreferencesAnalyticsEvents(
        AnalyticsNames.NOTIFICATIONS_CLOSED.eventName,
        mapOf(
            KEY_CLOSING_METHOD to KEY_BACK
        )
    )

    data class ChannelSetUpEvent(
        private val eventName: String,
        private val methods: List<ContactMethod>
    ) : NotificationPreferencesAnalyticsEvents(
        event = eventName,
        params = methods.associate {
            it.method to if (it.isMethodEnabled) KEY_ENABLED else KEY_DISABLED
        }
    )

    data class StatusChangeError(val channel: String) : NotificationPreferencesAnalyticsEvents(
        AnalyticsNames.NOTIFICATION_STATUS_CHANGE_ERROR.eventName,
        mapOf(
            KEY_ORIGIN to channel
        )
    )

    companion object {
        private const val KEY_OPTION_SELECTED = "option_selection"
        private const val KEY_OPTION_VIEWED = "option_viewed"

        private const val KEY_ORIGIN = "origin"

        private const val KEY_BACK = "BACK_BUTTON"

        private const val KEY_ENABLED = "Enable"
        private const val KEY_DISABLED = "Disable"

        private const val KEY_CLOSING_METHOD = "closing_method"

        private const val KEY_CATEGORY_NEWS = "NEWS"
        private const val KEY_CATEGORY_PRICE_ALERTS = "PRICE_ALERTS"
        private const val KEY_CATEGORY_SECURITY_ALERTS = "SECURITY_ALERTS"
        private const val KEY_CATEGORY_WALLET_ACTIVITY = "WALLET_ACTIVITY"

        fun createChannelSetUpEvent(channel: String, methods: List<ContactMethod>): ChannelSetUpEvent {
            val eventName = when (channel) {
                KEY_CATEGORY_NEWS -> AnalyticsNames.NOTIFICATION_NEWS_SET_UP.eventName
                KEY_CATEGORY_PRICE_ALERTS -> AnalyticsNames.NOTIFICATION_PRICE_ALERTS_SET_UP.eventName
                KEY_CATEGORY_SECURITY_ALERTS -> AnalyticsNames.NOTIFICATION_SECURITY_ALERTS_SET_UP.eventName
                KEY_CATEGORY_WALLET_ACTIVITY -> AnalyticsNames.NOTIFICATION_WALLET_ACTIVITY_SET_UP.eventName
                else -> channel
            }
            return ChannelSetUpEvent(eventName, methods)
        }
    }
}
