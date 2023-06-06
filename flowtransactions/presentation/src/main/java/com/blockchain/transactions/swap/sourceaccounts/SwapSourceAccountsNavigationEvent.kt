package com.blockchain.transactions.swap.sourceaccounts

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.transactions.common.CryptoAccountWithBalance

sealed interface SwapSourceAccountsNavigationEvent : NavigationEvent {
    data class ConfirmSelection(
        val account: CryptoAccountWithBalance,
        val requiresSecondPassword: Boolean,
    ) : SwapSourceAccountsNavigationEvent
}
