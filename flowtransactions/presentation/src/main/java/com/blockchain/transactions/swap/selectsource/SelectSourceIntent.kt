package com.blockchain.transactions.swap.selectsource

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.transactions.swap.enteramount.EnterAmountModelState

sealed interface SelectSourceIntent : Intent<SelectSourceModelState> {
    object LoadData : SelectSourceIntent
}