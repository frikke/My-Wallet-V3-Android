package com.blockchain.componentlib.utils

import com.blockchain.analytics.Analytics
import com.blockchain.analytics.AnalyticsEvent

val previewAnalytics = object : Analytics {
    override fun logEvent(analyticsEvent: AnalyticsEvent) {}
    override fun logEventOnce(analyticsEvent: AnalyticsEvent) {}
    override fun logEventOnceForSession(analyticsEvent: AnalyticsEvent) {}
}
