package com.blockchain.transactions.swap.selecttarget

import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.transactions.swap.CryptoAccountWithBalance

sealed interface TargetAssetNavigationEvent : NavigationEvent {
    data class ConfirmSelection(val account: CryptoAccount) : TargetAssetNavigationEvent
    data class SelectAccount(val ofTicker: String) : TargetAssetNavigationEvent
}
