package com.blockchain.transactions.swap.enteramount

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface EnterAmountIntent : Intent<EnterAmountModelState> {
    object LoadData : EnterAmountIntent

    object FlipInputs : EnterAmountIntent

    data class FiatInputChanged(
        val amount: String
    ) : EnterAmountIntent

    data class CryptoInputChanged(
        val amount: String
    ) : EnterAmountIntent

    data class FromAccountChanged(
        val ticker: String
    ) : EnterAmountIntent

    object MaxSelected : EnterAmountIntent

    object PreviewClicked : EnterAmountIntent
}
