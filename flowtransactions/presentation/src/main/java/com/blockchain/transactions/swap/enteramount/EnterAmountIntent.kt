package com.blockchain.transactions.swap.enteramount

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface EnterAmountIntent : Intent<EnterAmountModelState> {
    object LoadData : EnterAmountIntent

    object FlipInputs : EnterAmountIntent

    object PreviewClicked : EnterAmountIntent

    data class FiatAmountChanged(
        val amount: String
    ) : EnterAmountIntent

    data class CryptoAmountChanged(
        val amount: String
    ) : EnterAmountIntent
}
