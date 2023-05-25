package com.blockchain.transactions.sell.enteramount

import com.blockchain.coincore.FiatAccount
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.presentation.complexcomponents.QuickFillDisplayAndAmount
import com.blockchain.transactions.common.CryptoAccountWithBalance

sealed interface EnterAmountIntent : Intent<EnterAmountModelState> {
    object FlipInputs : EnterAmountIntent

    data class FiatInputChanged(
        val amount: String
    ) : EnterAmountIntent

    data class CryptoInputChanged(
        val amount: String
    ) : EnterAmountIntent

    data class FromAccountChanged(
        val account: CryptoAccountWithBalance,
        val secondPassword: String?,
    ) : EnterAmountIntent {
        override fun isValidFor(modelState: EnterAmountModelState): Boolean {
            return modelState.fromAccount?.account?.matches(account.account) != true
        }
    }

    data class FromAndToAccountsChanged(
        val fromAccount: CryptoAccountWithBalance,
        val secondPassword: String?,
        val toAccount: FiatAccount
    ) : EnterAmountIntent {
        override fun isValidFor(modelState: EnterAmountModelState): Boolean {
            return modelState.toAccount?.currency != toAccount.currency ||
                modelState.fromAccount?.account?.matches(fromAccount.account) != true ||
                modelState.secondPassword != secondPassword
        }
    }

    data class QuickFillEntryClicked(val entry: QuickFillDisplayAndAmount) : EnterAmountIntent

    object MaxSelected : EnterAmountIntent

    object PreviewClicked : EnterAmountIntent

    object SnackbarErrorHandled : EnterAmountIntent
}
