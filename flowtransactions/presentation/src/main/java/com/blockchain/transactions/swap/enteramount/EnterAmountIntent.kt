package com.blockchain.transactions.swap.enteramount

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface EnterAmountIntent : Intent<EnterAmountModelState> {
    object LoadData : EnterAmountIntent

    data class FiatAmountChanged(
        val amount: String
    ) : EnterAmountIntent {
        override fun isValidFor(modelState: EnterAmountModelState): Boolean {
            return modelState.fiatAmount != null && modelState.cryptoAmount != null
        }
    }

    data class CryptoAmountChanged(
        val amount: String
    ) : EnterAmountIntent {
        override fun isValidFor(modelState: EnterAmountModelState): Boolean {
            return modelState.fiatAmount != null && modelState.cryptoAmount != null
        }
    }
}
