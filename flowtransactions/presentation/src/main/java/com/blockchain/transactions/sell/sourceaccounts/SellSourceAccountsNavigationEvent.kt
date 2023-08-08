package com.blockchain.transactions.sell.sourceaccounts

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.transactions.common.CryptoAccountWithBalance

sealed interface SellSourceAccountsNavigationEvent : NavigationEvent {
    data class ConfirmSelection(
        val account: CryptoAccountWithBalance,
        val requiresSecondPassword: Boolean,
    ) : SellSourceAccountsNavigationEvent
}
