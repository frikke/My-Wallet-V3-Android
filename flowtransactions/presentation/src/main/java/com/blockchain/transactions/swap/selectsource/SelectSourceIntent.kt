package com.blockchain.transactions.swap.selectsource

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource

sealed interface SelectSourceIntent : Intent<SelectSourceModelState> {
    object LoadData : SelectSourceIntent
    data class AccountSelected(val id: String) : SelectSourceIntent {
        override fun isValidFor(modelState: SelectSourceModelState): Boolean {
            return modelState.accountListData is DataResource.Data
        }
    }
}
