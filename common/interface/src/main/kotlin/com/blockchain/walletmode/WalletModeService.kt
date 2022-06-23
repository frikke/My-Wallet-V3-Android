package com.blockchain.walletmode

import io.reactivex.rxjava3.core.Observable

interface WalletModeService {
    fun enabledWalletMode(): WalletMode
    val walletMode: Observable<WalletMode>
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
