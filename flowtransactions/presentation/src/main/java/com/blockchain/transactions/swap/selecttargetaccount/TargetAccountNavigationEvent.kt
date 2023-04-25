package com.blockchain.transactions.swap.selecttargetaccount

import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.transactions.swap.CryptoAccountWithBalance

sealed interface TargetAccountNavigationEvent : NavigationEvent {
    data class ConfirmSelection(val account: CryptoAccount) : TargetAccountNavigationEvent
}
