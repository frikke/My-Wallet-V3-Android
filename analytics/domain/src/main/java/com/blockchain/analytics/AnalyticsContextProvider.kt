package com.blockchain.analytics

import com.blockchain.api.analytics.AnalyticsContext

interface AnalyticsContextProvider {
    fun context(experiments: Map<String, String>): AnalyticsContext
}
