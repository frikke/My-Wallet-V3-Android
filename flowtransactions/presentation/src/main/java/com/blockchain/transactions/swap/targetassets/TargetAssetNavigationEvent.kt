package com.blockchain.transactions.swap.targetassets

import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed interface TargetAssetNavigationEvent : NavigationEvent {
    data class ConfirmSelection(val account: CryptoAccount) : TargetAssetNavigationEvent
    data class SelectAccount(val ofTicker: String) : TargetAssetNavigationEvent
}
