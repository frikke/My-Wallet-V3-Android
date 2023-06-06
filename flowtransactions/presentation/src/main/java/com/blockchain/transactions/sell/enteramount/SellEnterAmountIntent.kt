package com.blockchain.transactions.sell.enteramount

import com.blockchain.coincore.FiatAccount
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.componentlib.keyboard.KeyboardButton
import com.blockchain.presentation.complexcomponents.QuickFillDisplayAndAmount
import com.blockchain.transactions.common.CryptoAccountWithBalance

sealed interface SellEnterAmountIntent : Intent<SellEnterAmountModelState> {
    object FlipInputs : SellEnterAmountIntent

    data class KeyboardClicked(
        val button: KeyboardButton
    ) : SellEnterAmountIntent

    data class FromAccountChanged(
        val account: CryptoAccountWithBalance,
        val secondPassword: String?,
    ) : SellEnterAmountIntent {
        override fun isValidFor(modelState: SellEnterAmountModelState): Boolean {
            return modelState.fromAccount?.account?.matches(account.account) != true
        }
    }

    data class FromAndToAccountsChanged(
        val fromAccount: CryptoAccountWithBalance,
        val secondPassword: String?,
        val toAccount: FiatAccount
    ) : SellEnterAmountIntent {
        override fun isValidFor(modelState: SellEnterAmountModelState): Boolean {
            return modelState.toAccount?.currency != toAccount.currency ||
                modelState.fromAccount?.account?.matches(fromAccount.account) != true ||
                modelState.secondPassword != secondPassword
        }
    }

    data class QuickFillEntryClicked(val entry: QuickFillDisplayAndAmount) : SellEnterAmountIntent

    object MaxSelected : SellEnterAmountIntent

    object PreviewClicked : SellEnterAmountIntent

    object SnackbarErrorHandled : SellEnterAmountIntent
}
