package com.blockchain.walletmode

import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.rx3.asObservable

interface WalletModeService {
    val walletMode: Flow<WalletMode>

    val walletModeSingle: Single<WalletMode>
        get() = walletMode.asObservable().firstOrError()

    fun reset()
    suspend fun updateEnabledWalletMode(type: WalletMode)
    fun availableModes(): List<WalletMode>
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

interface WalletModeStore {
    fun updateWalletMode(walletMode: WalletMode)
    val walletMode: WalletMode?
}
