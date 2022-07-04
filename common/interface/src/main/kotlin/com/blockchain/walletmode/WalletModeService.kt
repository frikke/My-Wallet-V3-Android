package com.blockchain.walletmode

import kotlinx.coroutines.flow.Flow

interface WalletModeService {
    fun enabledWalletMode(): WalletMode
    val walletMode: Flow<WalletMode>
    fun updateEnabledWalletMode(type: WalletMode)
}

enum class WalletMode {
    CUSTODIAL_ONLY,
    NON_CUSTODIAL_ONLY,
    UNIVERSAL;

    val custodialEnabled: Boolean
        get() = this == CUSTODIAL_ONLY || this == UNIVERSAL

    val nonCustodialEnabled: Boolean
        get() = this == NON_CUSTODIAL_ONLY || this == UNIVERSAL
}
