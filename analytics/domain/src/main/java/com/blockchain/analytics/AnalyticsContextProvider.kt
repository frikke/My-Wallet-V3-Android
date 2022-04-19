package com.blockchain.analytics

import com.blockchain.api.analytics.AnalyticsContext

interface AnalyticsContextProvider {
    fun context(): AnalyticsContext
}
