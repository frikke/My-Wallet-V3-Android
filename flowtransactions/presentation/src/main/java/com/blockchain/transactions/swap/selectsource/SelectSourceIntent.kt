package com.blockchain.transactions.swap.selectsource

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface SelectSourceIntent : Intent<SelectSourceModelState> {
    data class LoadData(val excludeAccountTicker: String? = null) : SelectSourceIntent
}
