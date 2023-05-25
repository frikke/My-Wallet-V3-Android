package com.blockchain.transactions.common.sourceaccounts

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.transactions.common.CryptoAccountWithBalance

sealed interface SourceAccountsNavigationEvent : NavigationEvent {
    data class ConfirmSelection(
        val account: CryptoAccountWithBalance,
        val requiresSecondPassword: Boolean,
    ) : SourceAccountsNavigationEvent
}
