package com.blockchain.transactions.swap.selecttarget

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.transactions.swap.selectsource.SelectSourceModelState

sealed interface SelectTargetIntent : Intent<SelectTargetModelState> {
    object LoadData : SelectTargetIntent
}
