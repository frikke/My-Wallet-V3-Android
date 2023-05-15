package com.blockchain.transactions.swap.enteramount

import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.transactions.swap.CryptoAccountWithBalance

sealed interface EnterAmountIntent : Intent<EnterAmountModelState> {
    object FlipInputs : EnterAmountIntent

    data class FiatInputChanged(
        val amount: String
    ) : EnterAmountIntent

    data class CryptoInputChanged(
        val amount: String
    ) : EnterAmountIntent

    data class FromAccountChanged(
        val account: CryptoAccountWithBalance
    ) : EnterAmountIntent {
        override fun isValidFor(modelState: EnterAmountModelState): Boolean {
            return modelState.fromAccount?.account?.matches(account.account) != true
        }
    }

    data class ToAccountChanged(
        val account: CryptoAccount
    ) : EnterAmountIntent {
        override fun isValidFor(modelState: EnterAmountModelState): Boolean {
            return modelState.toAccount?.matches(account) != true
        }
    }

    object MaxSelected : EnterAmountIntent

    object PreviewClicked : EnterAmountIntent

    object SnackbarErrorHandled : EnterAmountIntent
}
