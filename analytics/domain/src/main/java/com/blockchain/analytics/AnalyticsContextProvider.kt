package com.blockchain.analytics

import com.blockchain.walletmode.WalletMode

interface AnalyticsContextProvider {
    suspend fun context(overrideWalletMode: WalletMode? = null): AnalyticsContext
}
