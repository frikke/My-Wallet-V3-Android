package com.blockchain.transactions.swap.selecttargetaccount

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource

sealed interface SelectTargetAccountIntent : Intent<SelectTargetAccountModelState> {
    object LoadData : SelectTargetAccountIntent

    data class AccountSelected(val id: String) : SelectTargetAccountIntent {
        override fun isValidFor(modelState: SelectTargetAccountModelState): Boolean {
            return modelState.accountListData is DataResource.Data
        }
    }
}
