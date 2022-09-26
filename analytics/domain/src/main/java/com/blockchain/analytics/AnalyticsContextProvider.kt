package com.blockchain.analytics

import com.blockchain.api.analytics.AnalyticsContext

interface AnalyticsContextProvider {
    suspend fun context(): AnalyticsContext
}
