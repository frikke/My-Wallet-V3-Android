package com.blockchain.transactions.swap.enteramount

import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.componentlib.keyboard.KeyboardButton
import com.blockchain.transactions.common.CryptoAccountWithBalance

sealed interface SwapEnterAmountIntent : Intent<SwapEnterAmountModelState> {
    object FlipInputs : SwapEnterAmountIntent

    data class KeyboardClicked(
        val button: KeyboardButton
    ) : SwapEnterAmountIntent

    data class FromAccountChanged(
        val account: CryptoAccountWithBalance,
        val secondPassword: String?,
    ) : SwapEnterAmountIntent {
        override fun isValidFor(modelState: SwapEnterAmountModelState): Boolean {
            return modelState.fromAccount?.account?.matches(account.account) != true
        }
    }

    data class ToAccountChanged(
        val account: CryptoAccount
    ) : SwapEnterAmountIntent {
        override fun isValidFor(modelState: SwapEnterAmountModelState): Boolean {
            return modelState.toAccount?.matches(account) != true
        }
    }

    object MaxSelected : SwapEnterAmountIntent

    object PreviewClicked : SwapEnterAmountIntent

    object SnackbarErrorHandled : SwapEnterAmountIntent
}
