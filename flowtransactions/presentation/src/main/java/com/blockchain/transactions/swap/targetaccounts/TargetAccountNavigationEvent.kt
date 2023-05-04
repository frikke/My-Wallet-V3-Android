package com.blockchain.transactions.swap.targetaccounts

import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed interface TargetAccountNavigationEvent : NavigationEvent {
    data class ConfirmSelection(val account: CryptoAccount) : TargetAccountNavigationEvent
}
