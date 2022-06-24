package com.blockchain.analytics

interface ProviderSpecificAnalytics {
    fun logSignUp(success: Boolean)
    fun logLogin(success: Boolean)
    fun logContentView(screen: String)
    fun logShare(share: String)
}
