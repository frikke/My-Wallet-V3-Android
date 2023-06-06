package com.blockchain.transactions.sell.enteramount

import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.transactions.common.CryptoAccountWithBalance
import info.blockchain.balance.CryptoValue

sealed interface SellEnterAmountNavigationEvent : NavigationEvent {
    data class TargetAssets(
        val sourceAccount: CryptoAccountWithBalance,
        val secondPassword: String?,
    ) : SellEnterAmountNavigationEvent

    data class Preview(
        val sourceAccount: CryptoAccount,
        val targetAccount: FiatAccount,
        val sourceCryptoAmount: CryptoValue,
        val secondPassword: String?
    ) : SellEnterAmountNavigationEvent
}
