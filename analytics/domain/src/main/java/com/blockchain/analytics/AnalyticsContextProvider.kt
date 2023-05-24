package com.blockchain.analytics

interface AnalyticsContextProvider {
    suspend fun context(): AnalyticsContext
}
