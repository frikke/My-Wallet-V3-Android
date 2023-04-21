package com.blockchain.transactions.swap.selectsource

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.transactions.swap.CryptoAccountWithBalance

sealed interface SelectSourceNavigationEvent : NavigationEvent {
    data class ConfirmSelection(val account: CryptoAccountWithBalance) : SelectSourceNavigationEvent
}
