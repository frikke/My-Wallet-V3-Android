package com.blockchain.analytics.data

import com.blockchain.analytics.Analytics
import com.blockchain.analytics.AnalyticsEvent
import org.koin.android.ext.android.get

fun android.content.ComponentCallbacks.logEvent(analyticsEvent: AnalyticsEvent) {
    get<Analytics>().logEvent(analyticsEvent)
}
