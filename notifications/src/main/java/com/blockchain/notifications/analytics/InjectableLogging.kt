package com.blockchain.notifications.analytics

import android.content.Context
import android.os.Bundle
import com.blockchain.logging.CustomEventBuilder
import com.blockchain.logging.EventLogger
import com.google.firebase.analytics.FirebaseAnalytics

@Deprecated("Use com.blockchain.notifications.analytics.Analytics instead")
class InjectableLogging(context: Context) : EventLogger {
    private var analytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    override fun logEvent(customEventBuilder: CustomEventBuilder) {
        val b = Bundle()
        customEventBuilder.build { key, value ->
            b.putString(key, value)
        }
        analytics.logEvent(customEventBuilder.eventName, b)
    }
}