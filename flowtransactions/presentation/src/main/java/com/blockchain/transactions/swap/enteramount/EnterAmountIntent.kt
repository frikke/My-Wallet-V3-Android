package com.blockchain.transactions.swap.enteramount

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface EnterAmountIntent : Intent<EnterAmountModelState> {
    object LoadData : EnterAmountIntent
}
