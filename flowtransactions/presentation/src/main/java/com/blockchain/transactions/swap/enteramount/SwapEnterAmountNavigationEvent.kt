package com.blockchain.transactions.swap.enteramount

import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import info.blockchain.balance.CryptoValue

sealed interface SwapEnterAmountNavigationEvent : NavigationEvent {
    data class Preview(
        val sourceAccount: CryptoAccount,
        val targetAccount: CryptoAccount,
        val sourceCryptoAmount: CryptoValue,
        val secondPassword: String?
    ) : SwapEnterAmountNavigationEvent
}
