package com.blockchain.analytics

import com.blockchain.walletmode.WalletMode

interface TraitsService {
    suspend fun traits(overrideWalletMode: WalletMode? = null): Map<String, String>
}
