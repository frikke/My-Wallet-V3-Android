package com.blockchain.transactions.swap.targetassets

import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed interface SwapTargetAssetsNavigationEvent : NavigationEvent {
    data class ConfirmSelection(val account: CryptoAccount) : SwapTargetAssetsNavigationEvent
    data class SelectAccount(val ofTicker: String) : SwapTargetAssetsNavigationEvent
}
