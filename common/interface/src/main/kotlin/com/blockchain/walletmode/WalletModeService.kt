package com.blockchain.walletmode

interface WalletModeService {
    fun enabledWalletMode(): WalletMode
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
